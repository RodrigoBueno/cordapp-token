package br.com.tokens.contract

import br.com.tokens.common.groupStatesByLinearId
import br.com.tokens.state.BroadCastedState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

open class BroadCastedStateContract<T> : Contract {

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commandsOfType<Commands>().single()
        when (command.value) {
            is Commands.Broadcast -> verifyBroadcast(tx)
            is Commands.Create -> verifyCreate(tx)
        }
    }

    private fun verifyBroadcast(tx: LedgerTransaction) {
        requireThat {
            val inOuts = tx.groupStatesByLinearId<BroadCastedState<T>>()
            "Nenhum participante pode ser removido." using (
                    inOuts.all { it.output.participants.containsAll(it.input!!.participants) }
                    )
        }
    }

    private fun verifyCreate(tx: LedgerTransaction) {
        requireThat {
            "Nenhum State deve estar presente no input." using (tx.inputs.isEmpty())
            "Apenas um State pode ser criado por vez." using (tx.outputs.size == 1)
        }
    }

    interface Commands : CommandData {
        class Create : Commands
        class Broadcast : Commands
    }
}