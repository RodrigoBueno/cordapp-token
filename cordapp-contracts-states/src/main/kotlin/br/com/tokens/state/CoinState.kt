package br.com.tokens.state

import br.com.tokens.model.Coin
import br.com.tokens.schema.CoinSchemaV1
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class CoinState(
        private val coin: Coin,
        private val tokenId: UniqueIdentifier,
        override val linearId: UniqueIdentifier = UniqueIdentifier()
) : MultipleOwnersState<Coin>(coin, coin.participants.toList()), QueryableState {

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when(schema) {
            is CoinSchemaV1 -> CoinSchemaV1.PersistentCoin(
                    coin.owner.externalId,
                    tokenId.id.toString())
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(CoinSchemaV1)
}