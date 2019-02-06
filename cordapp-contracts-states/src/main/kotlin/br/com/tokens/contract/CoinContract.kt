package br.com.tokens.contract

import br.com.tokens.common.groupStatesByLinearId
import br.com.tokens.model.Coin
import br.com.tokens.state.CoinState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

class CoinContract : MultipleParticipantsContract<Coin>() {

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commandsOfType<MultipleParticipantsContract.Commands>().single()
        when (command.value) {
            is Commands.Transfer -> verifyTransfer(tx)
            is Commands.Burn -> verifyBurn(tx)
            else -> super.verify(tx)
        }
    }

    private fun verifyBurn(tx: LedgerTransaction) {
        requireThat {
            val inputs = tx.inputsOfType<CoinState>()
            "Todas as moedas devem ser do mesmo dono." using (
                    inputs.groupBy { it.value.owner }.size == 1 )
        }
    }

    private fun verifyTransfer(tx: LedgerTransaction) {
        requireThat {
            val inputs = tx.inputsOfType<CoinState>()
            val outputs = tx.outputsOfType<CoinState>()
            "Os valores de input e output precisam ser iguais." using (
                    inputs.sumBy { it.value.value } == outputs.sumBy { it.value.value })
            "Todas as moedas devem ser do mesmo dono." using (
                    inputs.groupBy { it.value.owner }.size == 1
                    )
        }
    }

    interface Commands : MultipleParticipantsContract.Commands {
        class Transfer : Commands
        class Burn : Commands
    }
}