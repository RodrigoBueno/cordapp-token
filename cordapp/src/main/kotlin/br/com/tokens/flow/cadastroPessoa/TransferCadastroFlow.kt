package br.com.tokens.flow.cadastroPessoa

import br.com.tokens.flow.account.AddPartAccountFlow
import br.com.tokens.flow.coin.AddPartCoinFlow
import br.com.tokens.schema.AccountSchemaV1
import br.com.tokens.schema.CadastroPessoaSchemaV1
import br.com.tokens.schema.CoinSchemaV1
import br.com.tokens.schema.DocumentoSchemaV1
import br.com.tokens.state.AccountState
import br.com.tokens.state.CadastroPessoaState
import br.com.tokens.state.CoinState
import br.com.tokens.state.DocumentoState
import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.unwrap

// Este flow tem que enviar tudo para o cara que pedir, vai rodar os flows de adicionar o cara como participante

object TransferCadastroFlow {

    @InitiatingFlow
    @StartableByRPC
    class Initiator(val documento: String, val password: String): FlowLogic<SignedTransaction>(){

        @Suspendable
        override fun call(): SignedTransaction {
            val documentoState = builder {
                val docIndex = DocumentoSchemaV1.PersistentDocumento::documento.equal(documento)
                val query = QueryCriteria.VaultCustomQueryCriteria(docIndex)
                serviceHub.vaultService.queryBy<DocumentoState>(query)
            }.states.single().state.data

            val otherPartySession = initiateFlow(documentoState.value.owners.first())
            otherPartySession.sendAndReceive<SignedTransaction>(DocAndPass(documento, password)).unwrap{
                return it
            }
        }

    }

    @InitiatedBy(Initiator::class)
    class Responder(val otherSession: FlowSession): FlowLogic<SignedTransaction>() {

        @Suspendable
        override fun call(): SignedTransaction {
            val docAndPass = otherSession.receive<DocAndPass>().unwrap { it }
            val state = builder {
                val docIndex = CadastroPessoaSchemaV1.PersistentCadastroPessoa::documento.equal(docAndPass.documento)
                val query = QueryCriteria.VaultCustomQueryCriteria(docIndex)
                serviceHub.vaultService.queryBy<CadastroPessoaState>(query)
            }.states.single()
            require(state.state.data.value.password == docAndPass.password)
            val signedTx = subFlow(AddPartCadastroPessoaFlow.Initiator(otherSession.counterparty, state))
            val accounts = builder {
                val docIndex = AccountSchemaV1.PersistentAccount::documento.equal(docAndPass.documento)
                val query = QueryCriteria.VaultCustomQueryCriteria(docIndex)
                serviceHub.vaultService.queryBy<AccountState>(query)
            }.states
            accounts.forEach {
                subFlow(AddPartAccountFlow.Initiator(otherSession.counterparty, it))
            }
            val coins = builder {
                val docIndex = CoinSchemaV1.PersistentCoin::userExternalId.equal(docAndPass.documento)
                val query = QueryCriteria.VaultCustomQueryCriteria(docIndex)
                serviceHub.vaultService.queryBy<CoinState>(query)
            }.states
            coins.forEach {
                subFlow(AddPartCoinFlow.Initiator(otherSession.counterparty, it))
            }
            otherSession.send(signedTx)
            return signedTx
        }
    }

    @CordaSerializable
    data class DocAndPass(val documento: String, val password: String)

}