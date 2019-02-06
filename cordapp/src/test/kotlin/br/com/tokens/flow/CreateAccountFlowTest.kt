package br.com.tokens.flow

import br.com.tokens.flow.account.CreateAccountFlow
import br.com.tokens.model.Account
import br.com.tokens.model.CadastroPessoa
import br.com.tokens.state.AccountState
import net.corda.core.contracts.StateAndRef
import net.corda.testing.node.MockNetwork
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class CreateAccountFlowTest {

    private val network = MockNetwork(listOf("br.com.tokens.contract"))
    private val a = network.createNode()
    private val b = network.createNode()
    private lateinit var state: StateAndRef<AccountState>

    init {
    }

    @Before
    fun setup() {
        network.runNetwork()
    }

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun `criar nova conta`() {
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
        assertEquals(account.externalId, state.state.data.value.externalId)
        assertEquals(account.userMetadata.nationalId, state.state.data.value.userMetadata.nationalId)
        assertEquals(account.userMetadata.name, state.state.data.value.userMetadata.name)
        assertEquals(account.userMetadata.email, state.state.data.value.userMetadata.email)
        assertEquals(account.userMetadata.phone, state.state.data.value.userMetadata.phone)
        assertEquals(account.userMetadata.password, state.state.data.value.userMetadata.password)
    }
}