package br.com.tokens.flow.account

import br.com.tokens.flow.abstract.CreateMultipleOwnersStateFlow
import br.com.tokens.model.Account
import br.com.tokens.state.AccountState
import br.com.tokens.state.MultipleOwnersState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC

object CreateAccountFlow {

    @InitiatingFlow
    @StartableByRPC
    class Initiator(account: Account) : CreateMultipleOwnersStateFlow<Account>(account) {
        override fun createState(value: Account): MultipleOwnersState<Account> {
            val linearId = UniqueIdentifier()
            return AccountState(value.copy(id = linearId.id.toString()), linearId)
        }
    }
}