package br.com.tokens.api

import br.com.tokens.api.dto.*
import br.com.tokens.flow.account.AddTokenAccountFlow
import br.com.tokens.flow.account.CreateAccountFlow
import br.com.tokens.flow.cadastroPessoa.CreateCadastroPessoaFlow
import br.com.tokens.flow.cadastroPessoa.TransferCadastroFlow
import br.com.tokens.flow.coin.BurnTokenFlow
import br.com.tokens.flow.coin.CreateCoinFlow
import br.com.tokens.flow.coin.TransferCoinFlow
import br.com.tokens.flow.documento.BroadcastDocumentoFlow
import br.com.tokens.flow.documento.CreateDocumentoFlow
import br.com.tokens.flow.token.BroadcastTokenFlow
import br.com.tokens.flow.token.CreateTokenFlow
import br.com.tokens.model.*
import br.com.tokens.schema.AccountSchemaV1
import br.com.tokens.schema.CoinSchemaV1
import br.com.tokens.schema.DocumentoSchemaV1
import br.com.tokens.state.AccountState
import br.com.tokens.state.CoinState
import br.com.tokens.state.DocumentoState
import br.com.tokens.state.TokenState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.Builder.equal
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger
import java.time.Instant
import java.util.*
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

// *****************
// * API Endpoints *
// *****************
@Path("bndes")
class TokensApi(val rpcOps: CordaRPCOps) {

    companion object {
        private val logger: Logger = loggerFor<TokensApi>()
    }

    val ourIdentity = rpcOps.nodeInfo().legalIdentities.first()

    @GET
    @Path("account/{externalId}/checkExists")
    @Produces(MediaType.APPLICATION_JSON)
    fun checkAccountExist(@PathParam("externalId") externalId: String): Response {
        return if (documentExists(externalId)) Response.ok(MessageDTO("Account exists for this external Id")).build()
        else Response.status(404).entity(MessageDTO("Account does not exists for this external Id")).build()
    }

    private fun documentExists(documento: String): Boolean {
        val documentoIndex = DocumentoSchemaV1.PersistentDocumento::documento.equal(documento)
        val query = QueryCriteria.VaultCustomQueryCriteria(documentoIndex)

        val documentos = rpcOps.vaultQueryBy<DocumentoState>(query).states.map { it.state.data }
        return documentos.isNotEmpty()
    }

    @POST
    @Path("account/{externalId}/transfer")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun requestAccountTransfer(@PathParam("externalId") externalId: String, pass: UserPasswordDTO): Response {
        if (externalId.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ErrorDTO(ErrorCode.MalformedRequest.value, Exception(), "Malformed request: external Id must be informed.\n")).build()
        }
        if (pass.userPassword.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(ErrorDTO(ErrorCode.Unauthorized.value, Exception(), "Malformed request: user password must be informed.\n")).build()
        }
        return try {
            val tx = rpcOps.startTrackedFlow(TransferCadastroFlow::Initiator, externalId, pass.userPassword).returnValue.getOrThrow()
            val accountId = findAccountBy(externalId).linearId.toString()
            val coins = findCoinsBy(externalId).map { it.value }
                    .groupBy { it.tokenId }
                    .map { TokenTransferDTO(it.key.toString(), it.value.sumBy { coin -> coin.value }) }
            Response.status(Response.Status.OK).entity(SuccessDTO(tx.id.toString(), TransferAccountDTO(accountId, coins))).build()
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            Response.status(Response.Status.BAD_REQUEST).entity(ErrorDTO(ErrorCode.Unknown.value, ex, "")).build()
        }
    }

    private fun findAccountBy(externalId: String): AccountState =
            rpcOps.vaultQueryBy<AccountState>(
                    QueryCriteria
                            .VaultCustomQueryCriteria(
                                    AccountSchemaV1.PersistentAccount::documento.equal(externalId)))
                    .states.single().state.data

    private fun findCoinsBy(externalId: String): Collection<CoinState> =
            rpcOps.vaultQueryBy<CoinState>(
                    QueryCriteria
                            .VaultCustomQueryCriteria(
                                    CoinSchemaV1.PersistentCoin::userExternalId.equal(externalId)))
                    .states.map { it.state.data }

    private fun createAccount(account: AccountDTO, desserializedAccount: Account, cadastroPessoa: CadastroPessoa): Response {
        if (account.externalId.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ErrorDTO(ErrorCode.MalformedRequest.value, Exception(), "Malformed Request: external id must be informed.")).build()
        }
        if (documentExists(account.externalId)) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ErrorDTO(ErrorCode.Duplicated.value, Exception(), "External Id already exists on the network.")).build()
        }
        val desserializedDocumento = Documento(account.externalId, setOf(ourIdentity), setOf(ourIdentity))
        return try {
            val docState = rpcOps.startTrackedFlow(CreateDocumentoFlow::Initiator, desserializedDocumento).returnValue.getOrThrow().tx.outputsOfType<DocumentoState>().single()
            builder {
                val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(docState.linearId))
                val state = rpcOps.vaultQueryBy<DocumentoState>(queryCriteria).states.single()
                val otherParties = (rpcOps.networkMapSnapshot() - rpcOps.nodeInfo()).map { it.legalIdentities.first() } - rpcOps.notaryIdentities()
                rpcOps.startTrackedFlow(BroadcastDocumentoFlow::Initiator, otherParties.toSet(), state)
            }
            rpcOps.startTrackedFlow(CreateCadastroPessoaFlow::Initiator, cadastroPessoa).returnValue.getOrThrow()
            val tx = rpcOps.startTrackedFlow(CreateAccountFlow::Initiator, desserializedAccount).returnValue.getOrThrow()
            Response.status(Response.Status.CREATED).entity(SuccessDTO(tx.id.toString(),
                    AccountIdDTO(tx.coreTransaction.outRefsOfType<AccountState>().single().state.data.linearId.toString()))).build()
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            Response.status(Response.Status.BAD_REQUEST).entity(ErrorDTO(ErrorCode.Unknown.value, ex, "")).build()
        }
    }

    @PUT
    @Path("account/addAccount")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun addAccount(account: AccountDTO): Response {
        val cadastroPessoa = CadastroPessoa(
                account.userMetadata.nationalId,
                account.userMetadata.name,
                account.userMetadata.email,
                account.userMetadata.phone,
                account.password,
                setOf(ourIdentity))
        val desserializedAccount =
                Account("", account.externalId, cadastroPessoa, setOf(ourIdentity), setOf())
        return createAccount(account, desserializedAccount, cadastroPessoa)
    }

    @PUT
    @Path("account/addPrimaryAccount")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun addPrimaryAccount(account: AccountDTO): Response {
        val cadastroPessoa = CadastroPessoa(
                account.userMetadata.nationalId,
                account.userMetadata.name,
                account.userMetadata.email,
                account.userMetadata.phone,
                account.password,
                setOf(ourIdentity))
        val desserializedAccount =
                Account("", account.externalId, cadastroPessoa, setOf(ourIdentity), setOf(), ourIdentity)
        return createAccount(account, desserializedAccount, cadastroPessoa)
    }


    @GET
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    fun getAccounts(): Response {
        return try {
            builder {
                val accounts = rpcOps.vaultQueryBy<AccountState>().states
                        .map { it.state.data }
                        .map {
                            AccountReturnDTO(it.linearId.toString(),
                                    it.value.externalId,
                                    UserMetadataDTO(it.value.userMetadata.nationalId,
                                            it.value.userMetadata.name,
                                            it.value.userMetadata.email,
                                            it.value.userMetadata.phone))
                        }
                Response.status(Response.Status.OK).entity(accounts).build()
            }
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            Response.status(Response.Status.BAD_REQUEST).entity(ErrorDTO(ErrorCode.Unknown.value, ex, "")).build()
        }
    }

    @PUT
    @Path("token/addToken")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun addToken(token: TokenDTO): Response {
        val desserializedToken = Token(
                token.symbol,
                token.name,
                rpcOps.nodeInfo().legalIdentities.first(),
                token.description,
                setOf(rpcOps.nodeInfo().legalIdentities.first()))
        return try {
            val tx = rpcOps.startTrackedFlow(CreateTokenFlow::Initiator, desserializedToken).returnValue.getOrThrow().coreTransaction
            val state = tx.outRefsOfType<TokenState>().single()
            val otherParties = (rpcOps.networkMapSnapshot() - rpcOps.nodeInfo()).map { it.legalIdentities.first() } - rpcOps.notaryIdentities()
            rpcOps.startTrackedFlow(BroadcastTokenFlow::Initiator, otherParties.toSet(), state)
            return Response.status(Response.Status.CREATED)
                    .entity(SuccessDTO(tx.id.toString(),
                            TokenMetadataDTO(state.state.data.linearId.toString()))).build()
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            Response.status(Response.Status.BAD_REQUEST).entity(ErrorDTO(ErrorCode.Unknown.value, ex, "")).build()
        }
    }

    //                          val idToken: String,
      //                        val symbol: String,
        //                      val name: String,
          //                    val owner: String,
            //                  val description: String,
              //                val volume: Int,
                //              val dailyVolume: Int,
                  //            val deposits: Int,
                    //          val withdrawls: Int,
                      //        val activeAccounts: Int)
    @GET
    @Path("token")
    @Produces(MediaType.APPLICATION_JSON)
    fun getTokens(): Response {
        return try {
            val accounts = rpcOps.vaultQueryBy<AccountState>().states.map { it.state.data }

            val tokens = rpcOps.vaultQueryBy<TokenState>()

            val activeAccounts = tokens.states.map { it.state.data }.map { AccountsSum(it.linearId.id,
                    accounts.filter {acc -> acc.value.tokens.contains(it.value) }.count()) }.map { it.tokenId to it.total }.toMap()

            val volumes = getAllCoinOutputs().groupBy { it.value.tokenId }

            val dailyVolumes = rpcOps.internalVerifiedTransactionsSnapshot().filter {
                it.coreTransaction.outRefsOfType<CoinState>().isNotEmpty() && it.inputs.isNotEmpty() }
                    .groupBy { it.coreTransaction.outputsOfType<CoinState>().first().value.tokenId }
                    .mapValues {
                        it.value.map {
                            val originalOwner = findInput(it.inputs.first())?.state?.data?.value?.owner
                            it.coreTransaction.outputsOfType<CoinState>().filter { it.value.owner != originalOwner }.sumBy { it.value.value } }
                                .sum() }

            val deposits = rpcOps.internalVerifiedTransactionsSnapshot().filter {
                it.coreTransaction.outRefsOfType<CoinState>().isNotEmpty() && it.inputs.isEmpty() }
                    .flatMap { it.coreTransaction.outputsOfType<CoinState>() }
                    .groupBy { it.value.tokenId }

            val withdrawls = rpcOps.internalVerifiedTransactionsSnapshot().filter {
                it.coreTransaction.outRefsOfType<CoinState>().sumBy { it.state.data.value.value } <
                        it.inputs.mapNotNull { findInput(it)  }.sumBy { it.state.data.value.value } }
                    .groupBy { findInput(it.coreTransaction.inputs.first())?.state?.data?.value?.tokenId }
                    .mapValues { it.value.map { it.inputs.mapNotNull { findInput(it) }.sumBy { it.state.data.value.value } -
                            it.coreTransaction.outputsOfType<CoinState>().sumBy { it.value.value } }.sum() }

            val returnTokens = tokens.states.map {

                val tokenId = it.state.data.linearId.id
                val token = it.state.data.value

                TokenReturnDTO(
                    token,
                    token,
                    it.state.data.linearId,
                    volumes[tokenId]?.sumBy { it.value.value } ?: 0,
                    dailyVolumes[tokenId] ?: 0,
                    deposits[tokenId]?.sumBy { it.value.value } ?: 0,
                        withdrawls[tokenId] ?: 0,
                        activeAccounts[tokenId] ?: 0)
            }


            return Response.status(Response.Status.OK).entity(returnTokens).build()
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            Response.status(Response.Status.BAD_REQUEST).entity(ErrorDTO(ErrorCode.Unknown.value, ex, "")).build()
        }
    }

    @GET
    @Path("coin")
    @Produces(MediaType.APPLICATION_JSON)
    fun getCoins(): Response {
        return try {
            val coins = rpcOps.vaultQueryBy<CoinState>()

            return Response.status(Response.Status.OK).entity(coins.states.map { it.state.data }).build()
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            Response.status(Response.Status.BAD_REQUEST).entity(ErrorDTO(ErrorCode.Unknown.value, ex, "")).build()
        }
    }

    @POST
    @Path("token/{idToken}/addFunds")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun addFunds(@PathParam("idToken") idToken: String, funds: FundsDTO): Response {
        if (funds.value <= 0)
            return Response.status(Response.Status.BAD_REQUEST).entity(ErrorDTO(ErrorCode.MalformedRequest.value, Exception(), "The value should be higher than 0.\n")).build()
        val uuidToken = UUID.fromString(idToken)
        val tokens = rpcOps.vaultQueryBy<TokenState>(QueryCriteria.LinearStateQueryCriteria(uuid = listOf(uuidToken))).states
        if (tokens.size != 1)
            return Response.status(Response.Status.BAD_REQUEST).entity(ErrorDTO(ErrorCode.MalformedRequest.value, Exception(), "Token $idToken not found.")).build()
        val token = tokens.single()
        var account = builder {
            val criteria = QueryCriteria.VaultCustomQueryCriteria(AccountSchemaV1.PersistentAccount::primaryFor.equal(ourIdentity.name.toString()))
            rpcOps.vaultQueryBy<AccountState>(criteria).states.single()
        }
        val coin = Coin(funds.value, token.state.data.value.symbol, UUID.fromString(idToken), account.state.data.value, setOf(ourIdentity))
        return try {
            rpcOps.startTrackedFlow(AddTokenAccountFlow::Initiator, account, token.state.data.value).returnValue.getOrThrow()
            account = builder {
                val criteria = QueryCriteria.VaultCustomQueryCriteria(AccountSchemaV1.PersistentAccount::primaryFor.equal(ourIdentity.name.toString()))
                rpcOps.vaultQueryBy<AccountState>(criteria).states.single()
            }
            val tx = rpcOps.startTrackedFlow(CreateCoinFlow::Initiator, account, coin, token.state.data).returnValue.getOrThrow()
            val balance = builder {
                val criteria = QueryCriteria.VaultCustomQueryCriteria(CoinSchemaV1.PersistentCoin::tokenId.equal(idToken))
                val customCriteria = criteria.and(QueryCriteria.VaultCustomQueryCriteria(CoinSchemaV1.PersistentCoin::userExternalId.equal(account.state.data.value.externalId)))
                rpcOps.vaultQueryBy<CoinState>(customCriteria).states.map { it.state.data }.sumBy { it.value.value }
            }
            return Response.status(Response.Status.OK).entity(
                    SuccessDTO(
                            tx.id.toString(),
                            WalletTokenDTO(
                                    account.state.data.linearId.id.toString(),
                                    idToken,
                                    balance))).build()
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            Response.status(Response.Status.BAD_REQUEST).entity(ex.message!!).build()
        }
    }

    @POST
    @Path("account/{id_account}/token/{id_token}/transfer")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun transfer(@PathParam("id_account") idAccount: String,
                 @PathParam("id_token") idToken: String,
                 transferRequest: TransferDTO): Response {
        return try {
            val accountUUID = UUID.fromString(idAccount)
            val tokenUUID = UUID.fromString(idToken)
            val accountToUUID = UUID.fromString(transferRequest.to)
            val accountFrom = getAccount(accountUUID)
            val accountTo = getAccount(accountToUUID)
            val token = getToken(tokenUUID)

            val tx = rpcOps.startTrackedFlow(
                    TransferCoinFlow::Initiator,
                    accountFrom,
                    accountTo,
                    token.linearId.id,
                    transferRequest.value).returnValue.getOrThrow()

            val accountFromBalance = queryBalance(tokenUUID, accountFrom.value.externalId)
            val accountToBalance = queryBalance(tokenUUID, accountTo.value.externalId)
            return Response.status(Response.Status.OK).entity(SuccessDTO(tx.id.toString(),
                    TransferMetadataDTO(idToken, accountFromBalance, accountToBalance))).build()
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            Response.status(Response.Status.BAD_REQUEST).entity(ex.message!!).build()
        }
    }

    @POST
    @Path("account/{id_account}/token/{id_token}/burn")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun burn(@PathParam("id_account") idAccount: String,
                 @PathParam("id_token") idToken: String,
                 burnRequest: BurnDTO): Response {
        return try {
            val accountUUID = UUID.fromString(idAccount)
            val tokenUUID = UUID.fromString(idToken)
            val account = getAccount(accountUUID)
            val token = getToken(tokenUUID)

            val tx = rpcOps.startTrackedFlow(
                    BurnTokenFlow::Initiator,
                    account,
                    token.linearId.id,
                    burnRequest.ammount
            ).returnValue.getOrThrow()

            val accountBalance = queryBalance(tokenUUID, account.value.externalId)
            return Response.status(Response.Status.OK).entity(SuccessDTO(tx.id.toString(),
                    BurnReturnDTO(accountBalance))).build()
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            Response.status(Response.Status.BAD_REQUEST).entity(ex.message!!).build()
        }
    }

//    @Path("/api/token/{idToken}/history?days={days}&pageSize={pageSize}&page={page}")
//    @Produces(MediaType.APPLICATION_JSON)
//    fun history(@QueryParam("idToken") idToken: String,
//                @QueryParam("days") days: Int,
//                @QueryParam("pageSize") pageSize: Int,
//                @QueryParam("page") page: Int): Response {


//
//
//        val txs = rpcOps.internalVerifiedTransactionsSnapshot().filter { it.coreTransaction.outputStates.all { it is CoinState } }
//        val pages = txs.size.div(pageSize)
//
//
//        return Response.ok().entity(HistoryDTO(
//                pages = pages,
//                page = page,
//                records = txs.map {
//
//                    val inputs = it.coreTransaction.inputs.map { findInput(it) }
//                    val outputs = it.coreTransaction.outRefsOfType<CoinState>()
//                    val from = if (inputs.isNotEmpty()) inputs.first().state.data.value.owner.id else ""
//
//                    HistoryRecordDTO(
//                        transactionId = it.id.toString(),
//                        from = from,
//                        to = if (outputs.isNotEmpty()) outputs.firstOrNull { it.state.data.value.owner.id != from }!!.state.data.value.owner.id else "",
//                        value = Math.abs(inputs.sumBy { it.state.data.value.value } - it.coreTransaction.outputsOfType<CoinState>().sumBy { it.value.value }),
//                            timeStamp = (inputs + outputs).first()
//                ) }
//        )).build()
//        TODO()
//    }

    private fun getAllCoinOutputs(): List<CoinState> =
        rpcOps.vaultQueryBy<CoinState>().states.map { it.state.data }

    private fun findInput(ref : StateRef): StateAndRef<CoinState>? =
        rpcOps.vaultQueryBy<CoinState>(QueryCriteria.VaultQueryCriteria(stateRefs = listOf(ref))).states.firstOrNull()

    private fun queryBalance(tokenId: UUID, externalID: String): Int = builder {
        rpcOps.vaultQueryBy<CoinState>(QueryCriteria.VaultCustomQueryCriteria(CoinSchemaV1.PersistentCoin::tokenId.equal(tokenId.toString()))
                .and(QueryCriteria.VaultCustomQueryCriteria(CoinSchemaV1.PersistentCoin::userExternalId.equal(externalID))))
                .states.sumBy { it.state.data.value.value }
    }

    private fun getAccount(accountId: UUID): AccountState =
            rpcOps.vaultQueryBy<AccountState>(QueryCriteria.LinearStateQueryCriteria(uuid = listOf(accountId))).states.single().state.data

    private fun getToken(tokenId: UUID): TokenState =
            rpcOps.vaultQueryBy<TokenState>(QueryCriteria.LinearStateQueryCriteria(uuid = listOf(tokenId))).states.single().state.data

    data class AccountsSum(val tokenId: UUID, val total: Int)

}


