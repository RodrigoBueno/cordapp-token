package br.com.tokens.flow.documento

import br.com.tokens.flow.abstract.BroadcastStateFlow
import br.com.tokens.model.Documento
import br.com.tokens.state.DocumentoState
import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction

object BroadcastDocumentoFlow {

    @InitiatingFlow
    @StartableByRPC
    class Initiator(newParticipants: Set<Party>,
                    oldState: StateAndRef<DocumentoState>
    ) : BroadcastStateFlow<Documento, DocumentoState>(newParticipants, oldState) {
        override fun createNewState(oldState: DocumentoState): DocumentoState {
            return oldState.copy(
                    documento = oldState.value.copy(
                            participants = oldState.value.participants + newParticipants
                    )
            )
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