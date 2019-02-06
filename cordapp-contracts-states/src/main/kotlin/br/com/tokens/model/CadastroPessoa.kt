package br.com.tokens.model

import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class CadastroPessoa(
        val nationalId: String,
        val name: String,
        val email: String,
        val phone: String,
        val password: String,
        val owners: Set<Party>
)