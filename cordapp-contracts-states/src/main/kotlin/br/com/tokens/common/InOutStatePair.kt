package br.com.tokens.common

import br.com.tokens.state.MultipleOwnersState
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.transactions.LedgerTransaction

data class InOutStatePair<T: ContractState>(
        val input: T?,
        val output: T)

inline fun <reified T: LinearState> LedgerTransaction.groupStatesByLinearId(): Set<InOutStatePair<T>> =
        this.groupStates<T, UniqueIdentifier> { it.linearId }
                .map { InOutStatePair(it.inputs.singleOrNull(), it.outputs.single()) }.toSet()
