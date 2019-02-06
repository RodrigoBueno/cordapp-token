package br.com.tokens.model

import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class Token(
        val symbol: String,
        val name: String,
        val owner: Party,
        val description: String,
        val participants: Set<Party>
)