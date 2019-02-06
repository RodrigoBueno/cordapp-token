package br.com.tokens.flow.token

import br.com.tokens.flow.abstract.CreateBroadcastStateFlow
import br.com.tokens.model.Token
import br.com.tokens.state.MultipleOwnersState
import br.com.tokens.state.TokenState
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC

object CreateTokenFlow {

    @InitiatingFlow
    @StartableByRPC
    class Initiator(token: Token) : CreateBroadcastStateFlow<Token>(token) {
        override fun createState(value: Token): MultipleOwnersState<Token> {
            return TokenState(value)
        }
    }
}