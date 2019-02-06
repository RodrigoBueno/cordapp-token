package br.com.tokens.flow.token

import br.com.tokens.flow.abstract.BroadcastStateFlow
import br.com.tokens.model.Token
import br.com.tokens.state.TokenState
import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction

object BroadcastTokenFlow {

    @InitiatingFlow
    @StartableByRPC
    class Initiator(newParticipants: Set<Party>,
                    oldState: StateAndRef<TokenState>
    ) : BroadcastStateFlow<Token, TokenState>(newParticipants, oldState) {
        override fun createNewState(oldState: TokenState): TokenState {
            return oldState.copy(
                    token = oldState.value.copy(
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