package br.com.tokens.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

object AccountSchema

object AccountSchemaV1 : MappedSchema(
        schemaFamily = AccountSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentAccount::class.java)) {
    @Entity
    @Table(name = "account")
    class PersistentAccount(
            @Column(name = "userExternalId")
            var documento: String,
            var primaryFor: String
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this("", "")
    }
}