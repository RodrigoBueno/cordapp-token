package br.com.tokens.flow.abstract

import br.com.tokens.contract.MultipleParticipantsContract
import br.com.tokens.state.MultipleOwnersState
import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

abstract class AddOwnerToMultipleOwnerStateFlow<out K, T : MultipleOwnersState<K>>(
        val newOwner: Party,
        private val oldState: StateAndRef<T>) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        val notary = oldState.state.notary
        val command = Command(MultipleParticipantsContract.Commands.AddParticipant(),
                (oldState.state.data.participants + newOwner).map { it.owningKey })
        val newState = createNewState(oldState.state.data)
        val txBuilder = TransactionBuilder(notary)
                .addCommand(command)
                .addInputState(oldState)
                .addOutputState(newState, oldState.state.contract)
        txBuilder.verify(serviceHub)
        val allParties = newState.participants.mapNotNull {
            serviceHub.identityService.wellKnownPartyFromAnonymous(it)
        } - ourIdentity
        val sessions = allParties.map { initiateFlow(it) }
        val partSignTx = subFlow(CollectSignaturesFlow(serviceHub.signInitialTransaction(txBuilder), sessions))
        return subFlow(FinalityFlow(partSignTx))
    }

    abstract fun createNewState(oldState: T): T

}