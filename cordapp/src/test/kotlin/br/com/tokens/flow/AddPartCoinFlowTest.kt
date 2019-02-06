package br.com.tokens.flow

import br.com.tokens.flow.account.CreateAccountFlow
import br.com.tokens.flow.coin.AddPartCoinFlow
import br.com.tokens.flow.coin.CreateCoinFlow
import br.com.tokens.model.Account
import br.com.tokens.model.CadastroPessoa
import br.com.tokens.model.Coin
import br.com.tokens.model.Token
import br.com.tokens.state.AccountState
import br.com.tokens.state.CadastroPessoaState
import br.com.tokens.state.CoinState
import br.com.tokens.state.TokenState
import net.corda.core.contracts.StateAndRef
import net.corda.core.node.services.queryBy
import net.corda.testing.node.MockNetwork
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals

class AddPartCoinFlowTest {

    private val network = MockNetwork(listOf("br.com.tokens.contract"))
    private val a = network.createNode()
    private val b = network.createNode()
    private lateinit var state: StateAndRef<CoinState>
    private lateinit var accountState: StateAndRef<AccountState>

    init {
        listOf(a, b).forEach {
            it.registerInitiatedFlow(AddPartCoinFlow.Responder::class.java)
        }
    }

    @Before
    fun setup() {
        network.runNetwork()
        prepareData()
    }

    private fun prepareData() {
        createAccount()
        createCoin()
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

    private fun createCoin() {
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
    }

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun `deve adicionar um novo participante a moeda`() {
        val future = a.startFlow(AddPartCoinFlow.Initiator(newPart = b.info.legalIdentities.single(), oldState = state))
        network.runNetwork()
        future.get()
        listOf(a, b).forEach {
            it.transaction {
                val state = it.services.vaultService.queryBy<CoinState>().states.single().state.data
                assertEquals(state.value.participants.map { it.owningKey },
                        listOf(a.info.legalIdentities.first(), b.info.legalIdentities.first()).map { it.owningKey })
            }
        }
    }
}