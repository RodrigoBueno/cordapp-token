package br.com.tokens.state

import br.com.tokens.model.CadastroPessoa
import br.com.tokens.schema.CadastroPessoaSchemaV1
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class CadastroPessoaState(private val cadastroPessoa: CadastroPessoa,
                               override val linearId: UniqueIdentifier = UniqueIdentifier())
    : MultipleOwnersState<CadastroPessoa>(cadastroPessoa, cadastroPessoa.owners.toList()), QueryableState {

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is CadastroPessoaSchemaV1 -> CadastroPessoaSchemaV1.PersistentCadastroPessoa(
                    cadastroPessoa.nationalId
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(CadastroPessoaSchemaV1)

}