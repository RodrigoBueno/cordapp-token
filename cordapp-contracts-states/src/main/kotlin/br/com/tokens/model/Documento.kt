package br.com.tokens.model

import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class Documento(
        val value: String,
        val participants: Set<Party>,
        val owners: Set<Party>
)