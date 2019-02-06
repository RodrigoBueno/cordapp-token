package br.com.tokens.flow.account

import br.com.tokens.contract.AccountContract
import br.com.tokens.model.Token
import br.com.tokens.state.AccountState
import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

object AddTokenAccountFlow{

    @InitiatingFlow
    @StartableByRPC
    class Initiator(val accountRef: StateAndRef<AccountState>, val token: Token) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val accountState = accountRef.state.data
            val notary = serviceHub.networkMapCache.notaryIdentities.first()
            val command = Command(AccountContract.Commands.AddToken(), ourIdentity.owningKey)
            val newState = accountState.copy(account = accountState.value.copy(tokens = accountState.value.tokens + token))
            val txBuilder = TransactionBuilder(notary)
                    .addCommand(command)
                    .addInputState(accountRef)
                    .addOutputState(newState, AccountContract::class.java.canonicalName)

            txBuilder.verify(serviceHub)
            return subFlow(FinalityFlow(serviceHub.signInitialTransaction(txBuilder)))
        }
    }

    @InitiatedBy(Initiator::class)
    class Responder(val otherSession: FlowSession): FlowLogic<SignedTransaction>() {

        @Suspendable
        override fun call(): SignedTransaction {
            return subFlow(object: SignTransactionFlow(otherSession) {
                override fun checkTransaction(stx: SignedTransaction) {
                }
            })
        }
    }
}