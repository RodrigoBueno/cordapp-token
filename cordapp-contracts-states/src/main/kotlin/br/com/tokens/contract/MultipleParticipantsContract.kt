package br.com.tokens.contract

import br.com.tokens.common.InOutStatePair
import br.com.tokens.common.groupStatesByLinearId
import br.com.tokens.state.MultipleOwnersState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

open class MultipleParticipantsContract<T> : Contract {

    override fun verify(tx: LedgerTransaction) {
        val inOuts = tx.groupStatesByLinearId<MultipleOwnersState<T>>()
        val command = tx.commandsOfType<Commands>().single()
        when (command.value) {
            is Commands.Create -> verifyCreate(tx)
            is Commands.AddParticipant -> verifyAdd(inOuts)
            is Commands.RemoveParticipant -> verifyRemove(inOuts)
        }
        requireThat {
            "Todos os participantes devem assinar." using (inOuts.all {
                command.signers.containsAll((
                        if ( it.input == null )
                            it.output.participants
                        else it.input.participants + it.output.participants).map { participant -> participant.owningKey })
            })
        }
    }

    private fun verifyCreate(tx: LedgerTransaction) {
        requireThat {
            "Nenhum State deve estar presente no input." using (tx.inputs.isEmpty())
            "Apenas um State pode ser criado por vez." using (tx.outputs.size == 1)
        }
    }

    private fun verifyAdd(statesPairsByLinearId: Set<InOutStatePair<MultipleOwnersState<T>>>) {
        requireThat {
            "Deve haver um input." using (statesPairsByLinearId.all { it.input != null })
            "Deve ter acrescentado um novo participante." using (
                    statesPairsByLinearId.all {
                        it.input!!.participants.toSet().size + 1 == it.output.participants.toSet().size
                    }
                    )
            "Todos os demais participantes precisam continuar na lista." using (
                    statesPairsByLinearId.all {
                        it.output.participants.containsAll(it.input!!.participants)
                    }
                    )
        }
    }

    private fun verifyRemove(statesPairsByLinearId: Set<InOutStatePair<MultipleOwnersState<T>>>) {
        requireThat {
            "Deve haver um input." using (statesPairsByLinearId.all { it.input != null })
            "Deve ter removido um participante." using (
                    statesPairsByLinearId.all {
                        it.input!!.participants.toSet().size - 1 == it.output.participants.toSet().size
                    }
                    )
            "Todos os demais participantes precisam continuar na lista." using (
                    statesPairsByLinearId.all {
                        it.input!!.participants.containsAll(it.output.participants)
                    }
                    )
        }
    }

    interface Commands : CommandData {
        class Create : Commands
        class AddParticipant : Commands
        class RemoveParticipant : Commands
    }
}