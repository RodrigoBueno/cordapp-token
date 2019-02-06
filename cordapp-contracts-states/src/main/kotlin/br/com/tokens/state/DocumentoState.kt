package br.com.tokens.state

import br.com.tokens.model.Documento
import br.com.tokens.schema.DocumentoSchemaV1
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class DocumentoState(private val documento: Documento,
                          override val broadcastedValue: Documento = documento,
                          override val linearId: UniqueIdentifier = UniqueIdentifier()
) : MultipleOwnersState<Documento>(documento, documento.participants.toList()), BroadCastedState<Documento>, QueryableState {
    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when(schema) {
            is DocumentoSchemaV1 -> DocumentoSchemaV1.PersistentDocumento(documento.value)
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(DocumentoSchemaV1)
}