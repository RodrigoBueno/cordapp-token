package br.com.tokens.flow

import br.com.tokens.flow.documento.CreateDocumentoFlow
import br.com.tokens.model.Documento
import br.com.tokens.state.DocumentoState
import net.corda.core.contracts.StateAndRef
import net.corda.testing.node.MockNetwork
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class CreateDocumentoFlowTest {

    private val network = MockNetwork(listOf("br.com.tokens.contract"))
    private val a = network.createNode()
    private lateinit var state: StateAndRef<DocumentoState>

    init {
    }

    @Before
    fun setup() {
        network.runNetwork()
    }

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun `deve criar um novo documento na rede`() {
        val documento = Documento(
                value = "Teste",
                participants = setOf(a.info.legalIdentities.first()),
                owners = setOf(a.info.legalIdentities.first())
        )
        val future = a.startFlow(CreateDocumentoFlow.Initiator(documento))
        network.runNetwork()
        state = future.get().coreTransaction.outRefsOfType<DocumentoState>().single()

        assertEquals(documento, state.state.data.value)
    }
}