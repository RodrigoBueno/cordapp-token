package br.com.tokens.api.dto

data class TransferAccountDTO(val accountId: String, val tokens: List<TokenTransferDTO>): MetadataDTO