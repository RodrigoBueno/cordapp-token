package br.com.tokens.flow.account

import br.com.tokens.flow.abstract.AddOwnerToMultipleOwnerStateFlow
import br.com.tokens.model.Account
import br.com.tokens.state.AccountState
import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction

object AddPartAccountFlow {

    @InitiatingFlow
    @StartableByRPC
    class Initiator(val newPart: Party,
                    val oldState: StateAndRef<AccountState>
    ) : AddOwnerToMultipleOwnerStateFlow<Account, AccountState>(newPart, oldState) {
        override fun createNewState(oldState: AccountState): AccountState = oldState.copy(
                account = oldState.value.copy(
                        participants = oldState.value.participants + newPart))
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