package br.com.tokens.model

import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class Account(
        val id: String,
        val externalId: String,
        val userMetadata: CadastroPessoa,
        val participants: Set<Party>,
        val tokens: Set<Token>,
        val primaryFor: Party? = null
)