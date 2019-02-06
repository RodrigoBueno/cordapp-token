package br.com.tokens.flow.coin

import br.com.tokens.flow.abstract.AddOwnerToMultipleOwnerStateFlow
import br.com.tokens.model.CadastroPessoa
import br.com.tokens.model.Coin
import br.com.tokens.state.CadastroPessoaState
import br.com.tokens.state.CoinState
import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction

object AddPartCoinFlow {

    @InitiatingFlow
    @StartableByRPC
    class Initiator(val newPart: Party,
                    val oldState: StateAndRef<CoinState>
    ) : AddOwnerToMultipleOwnerStateFlow<Coin, CoinState>(newPart, oldState) {
        override fun createNewState(oldState: CoinState): CoinState = oldState.copy(
                coin = oldState.value.copy(
                        participants = oldState.value.participants + newPart))
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