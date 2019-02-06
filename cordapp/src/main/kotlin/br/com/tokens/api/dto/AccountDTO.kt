package br.com.tokens.api.dto

data class AccountDTO(
        val externalId: String,
        val password: String,
        val userMetadata: UserMetadataDTO
)