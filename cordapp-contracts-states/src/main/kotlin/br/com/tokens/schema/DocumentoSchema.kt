package br.com.tokens.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

object DocumentoSchema

object DocumentoSchemaV1 : MappedSchema(
        schemaFamily = DocumentoSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentDocumento::class.java)) {
    @Entity
    @Table(name = "userExternalId")
    class PersistentDocumento(
            @Column(name = "userExternalId")
            var documento: String
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this("")
    }
}