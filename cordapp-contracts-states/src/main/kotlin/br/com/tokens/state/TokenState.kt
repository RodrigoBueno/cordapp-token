package br.com.tokens.state

import br.com.tokens.model.Token
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class TokenState(private val token: Token,
                      override val broadcastedValue: Token = token,
                      override val linearId: UniqueIdentifier = UniqueIdentifier()
) : MultipleOwnersState<Token>(token, token.participants.toList()), BroadCastedState<Token>