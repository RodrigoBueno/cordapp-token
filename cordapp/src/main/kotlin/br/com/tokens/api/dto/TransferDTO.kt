package br.com.tokens.api.dto

data class TransferDTO(
        val to: String,
        val password: String,
        val value: Int)