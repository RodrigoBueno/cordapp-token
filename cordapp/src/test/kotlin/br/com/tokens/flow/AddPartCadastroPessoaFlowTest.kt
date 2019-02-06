package br.com.tokens.flow

import br.com.tokens.flow.cadastroPessoa.AddPartCadastroPessoaFlow
import br.com.tokens.flow.cadastroPessoa.CreateCadastroPessoaFlow
import br.com.tokens.model.CadastroPessoa
import br.com.tokens.state.CadastroPessoaState
import net.corda.core.contracts.StateAndRef
import net.corda.core.node.services.queryBy
import net.corda.testing.node.MockNetwork
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class AddPartCadastroPessoaFlowTest {

    private val network = MockNetwork(listOf("br.com.tokens.contract"))
    private val a = network.createNode()
    private val b = network.createNode()
    private lateinit var state: StateAndRef<CadastroPessoaState>

    init {
        listOf(a, b).forEach {
            it.registerInitiatedFlow(AddPartCadastroPessoaFlow.Responder::class.java)
        }
    }

    @Before
    fun setup() {
        network.runNetwork()
        prepareData()
    }

    private fun prepareData() {
        createCadastroPessoa()
    }

    private fun createCadastroPessoa() {
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
    }

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun `deve adicionar um novo dono ao cadastro da pessoa`() {
        val future = a.startFlow(AddPartCadastroPessoaFlow.Initiator(newPart = b.info.legalIdentities.single(), oldState = state))
        network.runNetwork()
        future.get()
        listOf(a, b).forEach {
            it.transaction {
                val state = a.services.vaultService.queryBy<CadastroPessoaState>().states.single().state.data
                assertEquals(state.value.owners.map { it.owningKey },
                        listOf(a.info.legalIdentities.first(), b.info.legalIdentities.first()).map { it.owningKey })
            }
        }
    }
}