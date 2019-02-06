package br.com.tokens.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

object CadastroPessoaSchema

object CadastroPessoaSchemaV1 : MappedSchema(
        schemaFamily = CadastroPessoaSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentCadastroPessoa::class.java)) {
    @Entity
    @Table(name = "cadastro_pessoa")
    class PersistentCadastroPessoa(
            @Column(name = "userExternalId")
            var documento: String
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this("")
    }
}