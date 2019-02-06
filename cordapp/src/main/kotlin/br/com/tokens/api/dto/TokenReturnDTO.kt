package br.com.tokens.api.dto

import br.com.tokens.model.Token
import net.corda.core.contracts.UniqueIdentifier

data class TokenReturnDTO(private val token: Token,
                          val broadcastedValue: Token = token,
                          val linearId: UniqueIdentifier,
                          val volume: Int,
                          val dailyVolume: Int,
                          val deposits: Int,
                          val withdrawls: Int,
                          val activeAccounts: Int)