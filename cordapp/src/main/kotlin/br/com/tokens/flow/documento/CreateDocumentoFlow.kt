package br.com.tokens.flow.documento

import br.com.tokens.flow.abstract.CreateBroadcastStateFlow
import br.com.tokens.model.Documento
import br.com.tokens.state.DocumentoState
import br.com.tokens.state.MultipleOwnersState
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC

object CreateDocumentoFlow {
    @InitiatingFlow
    @StartableByRPC
    class Initiator(documento: Documento) : CreateBroadcastStateFlow<Documento>(documento) {
        override fun createState(value: Documento): MultipleOwnersState<Documento> {
            return DocumentoState(value)
        }
    }
}