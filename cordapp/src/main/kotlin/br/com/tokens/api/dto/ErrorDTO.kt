package br.com.tokens.api.dto

data class ErrorDTO(val errorCode: Int, val exception: Throwable, val message: String)