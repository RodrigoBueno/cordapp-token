package br.com.tokens.state

import net.corda.core.contracts.LinearState
import net.corda.core.identity.AbstractParty
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
abstract class MultipleOwnersState<out T>(
        val value: T,
        owners: List<AbstractParty>
        ) : LinearState {
    override val participants: List<AbstractParty> = owners
}
