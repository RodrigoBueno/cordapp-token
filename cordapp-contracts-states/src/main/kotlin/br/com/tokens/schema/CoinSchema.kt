package br.com.tokens.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

object CoinSchema

/**
 * An IOUState schema.
 */
object CoinSchemaV1 : MappedSchema(
        schemaFamily = CoinSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentCoin::class.java)) {
    @Entity
    @Table(name = "coin")
    class PersistentCoin(
            @Column(name = "owner")
            var userExternalId: String,

            @Column(name = "token")
            var tokenId: String
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this("", "")
    }
}