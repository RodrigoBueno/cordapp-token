package br.com.tokens.flow

import br.com.tokens.flow.documento.BroadcastDocumentoFlow
import br.com.tokens.flow.documento.CreateDocumentoFlow
import br.com.tokens.model.Documento
import br.com.tokens.state.DocumentoState
import net.corda.core.contracts.StateAndRef
import net.corda.core.node.services.queryBy
import net.corda.testing.node.MockNetwork
import org.junit.After
import org.junit.Before
import org.junit.Test

class BroadcastDocumentoFlowTest {

    private val network = MockNetwork(listOf("br.com.tokens.contract"))
    private val a = network.createNode()
    private val b = network.createNode()
    private val c = network.createNode()
    private lateinit var state: StateAndRef<DocumentoState>

    init {
        listOf(a, b, c).forEach {
            it.registerInitiatedFlow(BroadcastDocumentoFlow.Responder::class.java)
        }
    }

    @Before
    fun setup() {
        network.runNetwork()
        prepareData()
    }

    private fun prepareData() {
        createDocumento()
    }

    private fun createDocumento() {
        val documento = Documento(
                value = "Teste",
                participants = setOf(a.info.legalIdentities.first()),
                owners = setOf(a.info.legalIdentities.first())
        )
        val future = a.startFlow(CreateDocumentoFlow.Initiator(documento))
        network.runNetwork()
        state = future.get().coreTransaction.outRefsOfType<DocumentoState>().single()
    }

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun `deve enviar o documento para todos os membros`() {
        val future = a.startFlow(BroadcastDocumentoFlow.Initiator(setOf(b.info.legalIdentities.first(), c.info.legalIdentities.first()), state))
        network.runNetwork()
        future.get()
        listOf(a, b, c).forEach {
            it.transaction {
                it.services.vaultService.queryBy<DocumentoState>().states.single().state.data
            }
        }
    }
}