package br.com.tokens.model

import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import java.util.*

@CordaSerializable
data class Coin(val value: Int,
                val tokenSymbol: String,
                val tokenId: UUID,
                val owner: Account,
                val participants: Set<Party>)