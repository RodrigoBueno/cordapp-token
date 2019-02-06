package br.com.tokens.flow

import br.com.tokens.flow.account.AddPartAccountFlow
import br.com.tokens.flow.account.CreateAccountFlow
import br.com.tokens.flow.cadastroPessoa.AddPartCadastroPessoaFlow
import br.com.tokens.flow.cadastroPessoa.CreateCadastroPessoaFlow
import br.com.tokens.flow.cadastroPessoa.TransferCadastroFlow
import br.com.tokens.flow.coin.AddPartCoinFlow
import br.com.tokens.flow.coin.CreateCoinFlow
import br.com.tokens.flow.coin.TransferCoinFlow
import br.com.tokens.flow.documento.BroadcastDocumentoFlow
import br.com.tokens.flow.documento.CreateDocumentoFlow
import br.com.tokens.flow.token.BroadcastTokenFlow
import br.com.tokens.flow.token.CreateTokenFlow
import br.com.tokens.model.*
import br.com.tokens.state.*
import net.corda.core.contracts.StateAndRef
import net.corda.core.node.services.queryBy
import net.corda.testing.node.MockNetwork
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals

class TransferCoinFlowTest {

    private val network = MockNetwork(listOf("br.com.tokens.contract", "br.com.tokens.schema"))
    private val a = network.createNode()
    private val b = network.createNode()
    private lateinit var tokenState: StateAndRef<TokenState>
    private lateinit var cadastroPessoaState: StateAndRef<CadastroPessoaState>
    private lateinit var documentoState: StateAndRef<DocumentoState>
    private lateinit var coinState: StateAndRef<CoinState>
    private lateinit var accountState: StateAndRef<AccountState>
    private lateinit var accountState2: StateAndRef<AccountState>


    init {
        listOf(a, b).forEach {
            it.registerInitiatedFlow(TransferCadastroFlow.Responder::class.java)
            it.registerInitiatedFlow(AddPartAccountFlow.Responder::class.java)
            it.registerInitiatedFlow(AddPartCadastroPessoaFlow.Responder::class.java)
            it.registerInitiatedFlow(AddPartCoinFlow.Responder::class.java)
            it.registerInitiatedFlow(BroadcastTokenFlow.Responder::class.java)
            it.registerInitiatedFlow(BroadcastDocumentoFlow.Responder::class.java)
            it.registerInitiatedFlow(TransferCoinFlow.Responder::class.java)
        }
    }

    @Before
    fun setup() {
        network.runNetwork()
        prepareData()
    }

    private fun prepareData() {
        createToken()
        createCadastroPessoa()
        createDocumento()
        createAccount()
        createAccount2()
        createCoin()
        sendCadastro()
    }

    private fun createToken() {
        val token = Token(name = "Teste",
                description = "Teste",
                symbol = "Teste",
                owner = a.info.legalIdentities.first(),
                participants = setOf(a.info.legalIdentities.first()))
        val future = a.startFlow(CreateTokenFlow.Initiator(token))
        network.runNetwork()
        tokenState = future.get().coreTransaction.outRefsOfType<TokenState>().single()
        val future2 = a.startFlow(BroadcastTokenFlow.Initiator(setOf(b.info.legalIdentities.first()), tokenState))
        network.runNetwork()
        future2.get()
    }

    private fun createCadastroPessoa() {
        val cadastroPessoa = CadastroPessoa(
                nationalId = "Teste",
                email = "Teste",
                name = "Teste",
                password = "Teste",
                phone = "Teste",
                owners = setOf(a.info.legalIdentities.first()))
        val future = a.startFlow(CreateCadastroPessoaFlow.Initiator(cadastroPessoa))
        network.runNetwork()
        cadastroPessoaState = future.get().coreTransaction.outRefsOfType<CadastroPessoaState>().single()
    }

    private fun createDocumento() {
        val documento = Documento(
                value = "Teste",
                participants = setOf(a.info.legalIdentities.first()),
                owners = setOf(a.info.legalIdentities.first())
        )
        val future = a.startFlow(CreateDocumentoFlow.Initiator(documento))
        network.runNetwork()
        documentoState = future.get().coreTransaction.outRefsOfType<DocumentoState>().single()
        val future2 = a.startFlow(BroadcastDocumentoFlow.Initiator(setOf(b.info.legalIdentities.first()), documentoState))
        network.runNetwork()
        future2.get()
    }

    private fun createAccount() {
        val account = Account(externalId = "Teste",
                participants = setOf(a.info.legalIdentities.first()),
                tokens = setOf(),
                userMetadata = cadastroPessoaState.state.data.value,
                id = "")
        val future = a.startFlow(CreateAccountFlow.Initiator(account))
        network.runNetwork()
        accountState = future.get().coreTransaction.outRefsOfType<AccountState>().single()
    }

    private fun createAccount2() {
        val account = Account(externalId = "Teste2",
                participants = setOf(a.info.legalIdentities.first()),
                tokens = setOf(),
                userMetadata = cadastroPessoaState.state.data.value,
                id = "")
        val future = a.startFlow(CreateAccountFlow.Initiator(account))
        network.runNetwork()
        accountState2 = future.get().coreTransaction.outRefsOfType<AccountState>().single()
    }

    private fun createCoin() {
        val coin = Coin(
                tokenId = UUID.randomUUID(),
                tokenSymbol = "SYM",
                value = 10,
                participants = setOf(a.info.legalIdentities.first()),
                owner = Account(externalId = "Teste",
                        participants = setOf(a.info.legalIdentities.first()),
                        tokens = setOf(),
                        userMetadata = cadastroPessoaState.state.data.value,
                        id = ""))
        val future = a.startFlow(CreateCoinFlow.Initiator(accountState, coin, tokenState.state.data))
        network.runNetwork()
        coinState = future.get().coreTransaction.outRefsOfType<CoinState>().single()
    }

    private fun sendCadastro() {
        val future = b.startFlow(TransferCadastroFlow.Initiator("Teste", "Teste"))
        network.runNetwork()
        future.get()
    }

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun `deve enviar o token para todos os membros`() {
        val future = b.startFlow(TransferCoinFlow.Initiator(accountState.state.data,
                accountState2.state.data,
                tokenState.state.data.linearId.id,
                5))
        network.runNetwork()
        future.get()
        listOf(a, b).forEach {
            it.transaction {
                it.services.vaultService.queryBy<TokenState>().states.single().state.data
                it.services.vaultService.queryBy<CadastroPessoaState>().states.single().state.data
                assertEquals(2, it.services.vaultService.queryBy<CoinState>().states.size)
                it.services.vaultService.queryBy<DocumentoState>().states.single().state.data
                assertEquals(2, it.services.vaultService.queryBy<AccountState>().states.size)
            }
        }
    }
}