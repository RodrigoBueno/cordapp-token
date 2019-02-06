package br.com.tokens.flow

import br.com.tokens.flow.documento.BroadcastDocumentoFlow
import br.com.tokens.flow.token.BroadcastTokenFlow
import br.com.tokens.flow.token.CreateTokenFlow
import br.com.tokens.model.Token
import br.com.tokens.state.DocumentoState
import br.com.tokens.state.TokenState
import net.corda.core.contracts.StateAndRef
import net.corda.core.node.services.queryBy
import net.corda.testing.node.MockNetwork
import org.junit.After
import org.junit.Before
import org.junit.Test

class BroadcastTokenFlowTest {

    private val network = MockNetwork(listOf("br.com.tokens.contract"))
    private val a = network.createNode()
    private val b = network.createNode()
    private lateinit var state: StateAndRef<TokenState>

    init {
        listOf(a, b).forEach {
            it.registerInitiatedFlow(BroadcastTokenFlow.Responder::class.java)
        }
    }

    @Before
    fun setup() {
        network.runNetwork()
        prepareData()
    }

    private fun prepareData() {
        createToken()
    }

    private fun createToken() {
        val token = Token(name = "Teste",
                description = "Teste",
                symbol = "Teste",
                owner = a.info.legalIdentities.first(),
                participants = setOf(a.info.legalIdentities.first()))
        val future = a.startFlow(CreateTokenFlow.Initiator(token))
        network.runNetwork()
        state = future.get().coreTransaction.outRefsOfType<TokenState>().single()

    }

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun `deve enviar o token para todos os membros`() {
        val future = a.startFlow(BroadcastTokenFlow.Initiator(setOf(b.info.legalIdentities.first()), state))
        network.runNetwork()
        future.get()
        listOf(a, b).forEach {
            it.transaction {
                it.services.vaultService.queryBy<TokenState>().states.single().state.data
            }
        }
    }
}