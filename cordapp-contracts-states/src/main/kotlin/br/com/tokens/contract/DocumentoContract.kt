package br.com.tokens.contract

import br.com.tokens.model.Documento

//O userExternalId deve ser divulgado para todos os membros da rede
class DocumentoContract: BroadCastedStateContract<Documento>()