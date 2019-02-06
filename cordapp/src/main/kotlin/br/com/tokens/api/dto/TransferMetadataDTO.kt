package br.com.tokens.api.dto

data class TransferMetadataDTO(val tokenId: String, val fromBalance: Int, val toBalance: Int): MetadataDTO