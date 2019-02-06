package br.com.tokens.flow.coin

import br.com.tokens.contract.CoinContract
import br.com.tokens.schema.CoinSchemaV1
import br.com.tokens.state.AccountState
import br.com.tokens.state.CoinState
import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.*

object BurnTokenFlow {

    @InitiatingFlow
    @StartableByRPC
    class Initiator(val account: AccountState, val tokenId: UUID, val value: Int) : FlowLogic<SignedTransaction>() {

        @Suspendable
        override fun call(): SignedTransaction {

            val accountTokens = builder {
                val criteria = QueryCriteria.VaultCustomQueryCriteria(CoinSchemaV1.PersistentCoin::tokenId.equal(tokenId.toString()))
                val criteria2 = QueryCriteria.VaultCustomQueryCriteria(CoinSchemaV1.PersistentCoin::userExternalId.equal(account.value.externalId))
                serviceHub.vaultService.queryBy<CoinState>(criteria.and(criteria2)).states
            }
            requireThat {
                "Deve haver saldo o suficiente" using (accountTokens.map { it.state.data.value.value }.sum() >= value)
            }
            var auxValue = value
            var auxValue2 : Int
            val selectedTokens = accountTokens.takeWhile {
                auxValue2 = auxValue
                auxValue -= it.state.data.value.value
                auxValue2 > 0
            }

            // Checa se tem troco
            val troco = selectedTokens.map { it.state.data.value.value }.sum() > value

            val notary = selectedTokens.first().state.notary
            val command = Command(CoinContract.Commands.Burn(),
                    selectedTokens.flatMap { it.state.data.value.participants.map { it.owningKey } })
            val txBuilder = TransactionBuilder(notary)
                    .addCommand(command)
            if (troco) {
                val stateRefComTroco = selectedTokens.last()
                val stateComTroco = stateRefComTroco.state.data
                val novaLista = selectedTokens.dropLast(1)
                val diferenca = value - novaLista.sumBy { it.state.data.value.value }
                val state1 = stateComTroco.copy(coin = stateComTroco.value.copy(value = stateComTroco.value.value - diferenca), linearId = UniqueIdentifier())
                txBuilder.addOutputState(state1, CoinContract::class.java.canonicalName)
                novaLista.map { it.state.data }.forEach {
                    val state = it.copy(coin = it.value.copy(owner = account.value), linearId = UniqueIdentifier())
                    txBuilder.addOutputState(state, CoinContract::class.java.canonicalName)
                }
                selectedTokens.forEach {
                    txBuilder.addInputState(it)
                }
            } else {
                selectedTokens.forEach {
                    txBuilder.addInputState(it)
                }
            }

            txBuilder.verify(serviceHub)

            var tx = serviceHub.signInitialTransaction(txBuilder)

            val sessions = selectedTokens.flatMap { it.state.data.value.participants }.minus(ourIdentity).map { initiateFlow(it) }
            if (sessions.isNotEmpty())
                tx = subFlow(CollectSignaturesFlow(tx, sessions))

            return subFlow(FinalityFlow(tx))
        }
    }

    @InitiatedBy(Initiator::class)
    class Responder(val otherSession: FlowSession) : FlowLogic<SignedTransaction>() {

        @Suspendable
        override fun call(): SignedTransaction {
            return subFlow(object : SignTransactionFlow(otherSession) {
                override fun checkTransaction(stx: SignedTransaction) {
                }
            })
        }
    }
}