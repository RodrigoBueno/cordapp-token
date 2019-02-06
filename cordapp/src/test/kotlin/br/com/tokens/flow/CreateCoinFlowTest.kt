package br.com.tokens.flow

import br.com.tokens.flow.account.CreateAccountFlow
import br.com.tokens.flow.coin.CreateCoinFlow
import br.com.tokens.model.Account
import br.com.tokens.model.CadastroPessoa
import br.com.tokens.model.Coin
import br.com.tokens.model.Token
import br.com.tokens.state.AccountState
import br.com.tokens.state.CoinState
import br.com.tokens.state.TokenState
import net.corda.core.contracts.StateAndRef
import net.corda.testing.node.MockNetwork
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals

class CreateCoinFlowTest {

    private val network = MockNetwork(listOf("br.com.tokens.contract"))
    private val a = network.createNode()
    private val b = network.createNode()
    private lateinit var state: StateAndRef<CoinState>
    private lateinit var accountState: StateAndRef<AccountState>

    init {
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
        accountState = future.get().coreTransaction.outRefsOfType<AccountState>().single()
    }

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun `deve criar uma nova moeda`() {
        val coin = Coin(
                tokenId = UUID.randomUUID(),
                tokenSymbol = "SYM",
                value = 10,
                participants = setOf(a.info.legalIdentities.first()),
                owner = Account(externalId = "Teste",
                        participants = setOf(a.info.legalIdentities.first()),
                        tokens = setOf(),
                        userMetadata = CadastroPessoa(
                                "123456",
                                "Teste",
                                "Teste",
                                "Teste",
                                "Teste",
                                setOf(a.info.legalIdentities.first())),
                        id = ""))
        val tokenState = TokenState(
                Token(
                        name = "Teste",
                        symbol = "Teste",
                        description = "Teste",
                        owner = a.info.legalIdentities.first(),
                        participants = setOf(a.info.legalIdentities.first(), b.info.legalIdentities.first())
                )
        )
        val future = a.startFlow(CreateCoinFlow.Initiator(accountState, coin, tokenState))
        network.runNetwork()
        state = future.get().coreTransaction.outRefsOfType<CoinState>().single()

        assertEquals(coin, state.state.data.value)
    }
}