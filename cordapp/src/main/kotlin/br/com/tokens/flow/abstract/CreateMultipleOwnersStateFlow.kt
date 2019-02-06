package br.com.tokens.flow.abstract

import br.com.tokens.contract.MultipleParticipantsContract
import br.com.tokens.state.MultipleOwnersState
import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.contracts.CommandData
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

abstract class CreateMultipleOwnersStateFlow<T>(val valueToCreate: T) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val command = Command(MultipleParticipantsContract.Commands.Create(), ourIdentity.owningKey)
        val outputState = createState(valueToCreate)
        val txBuilder = buildTransactionBuilder(notary, command, outputState)
        txBuilder.verify(serviceHub)
        return subFlow(FinalityFlow(serviceHub.signInitialTransaction(txBuilder)))
    }

    open fun <T, K : CommandData> buildTransactionBuilder(notary: Party, command: Command<K>, output: MultipleOwnersState<T>) =
            TransactionBuilder(notary)
                    .addCommand(command)
                    .addOutputState(output, MultipleParticipantsContract::class.java.canonicalName)

    abstract fun createState(value: T): MultipleOwnersState<T>

}