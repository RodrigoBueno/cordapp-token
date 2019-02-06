package br.com.tokens.flow

import br.com.tokens.flow.token.CreateTokenFlow
import br.com.tokens.model.Token
import br.com.tokens.state.TokenState
import net.corda.core.contracts.StateAndRef
import net.corda.testing.node.MockNetwork
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class CreateTokenFlowTest {

    private val network = MockNetwork(listOf("br.com.tokens.contract"))
    private val a = network.createNode()
    private lateinit var state: StateAndRef<TokenState>

    init {
    }

    @Before
    fun setup() {
        network.runNetwork()
    }

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun `deve criar um novo token`() {
        val token = Token(name = "Teste",
                description = "Teste",
                symbol = "Teste",
                owner = a.info.legalIdentities.first(),
                participants = setOf(a.info.legalIdentities.first()))
        val future = a.startFlow(CreateTokenFlow.Initiator(token))
        network.runNetwork()
        state = future.get().coreTransaction.outRefsOfType<TokenState>().single()

        assertEquals(token, state.state.data.value)
    }
}