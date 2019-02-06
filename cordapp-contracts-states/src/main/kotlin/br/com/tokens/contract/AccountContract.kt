package br.com.tokens.contract

import br.com.tokens.common.groupStatesByLinearId
import br.com.tokens.model.Account
import br.com.tokens.state.AccountState
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

// Validar regras de criacao de conta
class AccountContract : MultipleParticipantsContract<Account>() {

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commandsOfType<MultipleParticipantsContract.Commands>().single()
        when (command.value) {
            is Commands.AddToken -> verifyAddToken(tx)
            else -> super.verify(tx)
        }
    }

    fun verifyAddToken(tx: LedgerTransaction) {
        requireThat {
            val inputs = tx.inputsOfType<AccountState>()
            val outputs = tx.outputsOfType<AccountState>()
            val pairs = tx.groupStatesByLinearId<AccountState>()
            "Deve haver um input." using (inputs.size == 1)
            "Deve haver um output." using (outputs.size == 1)
            "Apenas a quantidade de tokens deve ser alterado." using (
                    pairs.all { it.input!!.value.tokens.size + 1 == it.output.value.tokens.size })
        }
    }

    interface Commands: MultipleParticipantsContract.Commands {
        class AddToken: Commands
    }

}