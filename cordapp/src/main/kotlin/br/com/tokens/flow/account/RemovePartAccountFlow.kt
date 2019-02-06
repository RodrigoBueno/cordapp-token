package br.com.tokens.flow.account

import br.com.tokens.flow.abstract.RemoveOwnerToMultipleStateFlow
import br.com.tokens.model.Account
import br.com.tokens.state.AccountState
import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction

object RemovePartAccountFlow {

    @InitiatingFlow
    @StartableByRPC
    class Initiator(val partToRemove: Party,
                    val oldState: StateAndRef<AccountState>
    ) : RemoveOwnerToMultipleStateFlow<Account, AccountState>(partToRemove, oldState) {
        override fun createNewState(oldState: AccountState): AccountState = oldState.copy(
                account = oldState.value.copy(
                        participants = oldState.value.participants - partToRemove))
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