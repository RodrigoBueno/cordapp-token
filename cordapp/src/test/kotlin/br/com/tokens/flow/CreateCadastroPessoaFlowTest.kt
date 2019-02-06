package br.com.tokens.flow

import br.com.tokens.flow.cadastroPessoa.CreateCadastroPessoaFlow
import br.com.tokens.model.CadastroPessoa
import br.com.tokens.state.CadastroPessoaState
import net.corda.core.contracts.StateAndRef
import net.corda.testing.node.MockNetwork
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class CreateCadastroPessoaFlowTest {

    private val network = MockNetwork(listOf("br.com.tokens.contract"))
    private val a = network.createNode()
    private lateinit var state: StateAndRef<CadastroPessoaState>

    init {
    }

    @Before
    fun setup() {
        network.runNetwork()
    }

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun `deve criar cadastro da pessoa`() {
        val cadastroPessoa = CadastroPessoa(
                nationalId = "Teste",
                email = "Teste",
                name = "Teste",
                password = "Teste",
                phone = "Teste",
                owners = setOf(a.info.legalIdentities.first()))
        val future = a.startFlow(CreateCadastroPessoaFlow.Initiator(cadastroPessoa))
        network.runNetwork()
        state = future.get().coreTransaction.outRefsOfType<CadastroPessoaState>().single()
        assertEquals(cadastroPessoa, state.state.data.value)
    }
}