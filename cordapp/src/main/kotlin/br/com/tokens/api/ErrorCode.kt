package br.com.tokens.api

enum class ErrorCode(val value: Int) {

    Unauthorized(1),
    NoFunds(2),
    MalformedRequest(5),
    Unknown(3),
    Duplicated(4)

}