package br.com.tokens.flow

import br.com.tokens.flow.account.AddTokenAccountFlow
import br.com.tokens.flow.account.CreateAccountFlow
import br.com.tokens.model.Account
import br.com.tokens.model.CadastroPessoa
import br.com.tokens.model.Token
import br.com.tokens.state.AccountState
import net.corda.core.contracts.StateAndRef
import net.corda.core.node.services.queryBy
import net.corda.testing.node.MockNetwork
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class AddTokenAccountFlowTest {

    private val network = MockNetwork(listOf("br.com.tokens.contract"))
    private val a = network.createNode()
    private val b = network.createNode()
    private lateinit var state: StateAndRef<AccountState>

    init {
        listOf(a, b).forEach {
            it.registerInitiatedFlow(AddTokenAccountFlow.Responder::class.java)
        }
    }

    @Before
    fun setup() {
        network.runNetwork()
        prepareData()
    }

    private fun prepareData() {
        createAccount()
    }

    private fun createAccount() {
        val account = Account(externalId = "Teste",
                participants = setOf(a.info.legalIdentities.first()),
                tokens = setOf(),
                userMetadata = CadastroPessoa(
                        "123456",
                        "Teste",
                        "Teste",
                        "Teste",
                        "Teste",
                        setOf(a.info.legalIdentities.first())),
                id = "")
        val future = a.startFlow(CreateAccountFlow.Initiator(account))
        network.runNetwork()
        state = future.get().coreTransaction.outRefsOfType<AccountState>().single()
    }

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun `deve adicionar um novo token à conta`() {
        val token = Token(
                name = "Teste",
                symbol = "Teste",
                description = "Teste",
                owner = a.info.legalIdentities.first(),
                participants = setOf(a.info.legalIdentities.first(), b.info.legalIdentities.first())
        )
        val future = a.startFlow(AddTokenAccountFlow.Initiator(state, token))
        network.runNetwork()
        future.get()
        a.transaction {
            val state = a.services.vaultService.queryBy<AccountState>().states.single().state.data
            assertEquals(1, state.value.tokens.size)
        }
    }
}