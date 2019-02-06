package br.com.tokens.flow.cadastroPessoa

import br.com.tokens.flow.abstract.CreateMultipleOwnersStateFlow
import br.com.tokens.model.CadastroPessoa
import br.com.tokens.state.CadastroPessoaState
import br.com.tokens.state.MultipleOwnersState
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC

object CreateCadastroPessoaFlow {
    @InitiatingFlow
    @StartableByRPC
    class Initiator(cadastroPessoa: CadastroPessoa) : CreateMultipleOwnersStateFlow<CadastroPessoa>(cadastroPessoa) {
        override fun createState(value: CadastroPessoa): MultipleOwnersState<CadastroPessoa> {
            return CadastroPessoaState(value)
        }
    }
}