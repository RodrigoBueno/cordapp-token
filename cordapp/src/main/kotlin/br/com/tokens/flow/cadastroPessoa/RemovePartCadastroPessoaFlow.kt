package br.com.tokens.flow.cadastroPessoa

import br.com.tokens.flow.abstract.RemoveOwnerToMultipleStateFlow
import br.com.tokens.model.CadastroPessoa
import br.com.tokens.state.CadastroPessoaState
import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction

object RemovePartCadastroPessoaFlow {

    @InitiatingFlow
    @StartableByRPC
    class Initiator(val partToRemove: Party,
                    val oldState: StateAndRef<CadastroPessoaState>
    ) : RemoveOwnerToMultipleStateFlow<CadastroPessoa, CadastroPessoaState>(partToRemove, oldState) {
        override fun createNewState(oldState: CadastroPessoaState): CadastroPessoaState = oldState.copy(
                cadastroPessoa = oldState.value.copy(
                        owners = oldState.value.owners - partToRemove))
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