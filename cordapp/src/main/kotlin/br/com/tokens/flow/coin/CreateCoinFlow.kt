package br.com.tokens.flow.coin

import br.com.tokens.contract.CoinContract
import br.com.tokens.contract.MultipleParticipantsContract
import br.com.tokens.flow.account.AddTokenAccountFlow
import br.com.tokens.model.Coin
import br.com.tokens.state.AccountState
import br.com.tokens.state.CoinState
import br.com.tokens.state.TokenState
import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

object CreateCoinFlow {

    @StartableByRPC
    @InitiatingFlow
    class Initiator(val account: StateAndRef<AccountState>, val coin: Coin, val token: TokenState): FlowLogic<SignedTransaction>() {

        @Suspendable
        override fun call(): SignedTransaction {
            val notary = serviceHub.networkMapCache.notaryIdentities.first()
            val command = Command(MultipleParticipantsContract.Commands.Create(), ourIdentity.owningKey)
            if (account.state.data.value.tokens.map { it.symbol }.none { it == coin.tokenSymbol })
                subFlow(AddTokenAccountFlow.Initiator(account, token.value))
            val coinState = CoinState(coin, token.linearId)
            val txBuilder = TransactionBuilder(notary)
                    .addCommand(command)
                    .addOutputState(coinState, CoinContract::class.java.canonicalName)

            txBuilder.verify(serviceHub)
            return subFlow(FinalityFlow(serviceHub.signInitialTransaction(txBuilder)))
        }
    }

}