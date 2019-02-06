package br.com.tokens.api

import net.corda.core.messaging.CordaRPCOps
import net.corda.webserver.services.WebServerPluginRegistry
import java.util.function.Function

// ***********
// * Plugins *
// ***********
class WebPlugin : WebServerPluginRegistry {
    override val webApis: List<java.util.function.Function<CordaRPCOps, out Any>> = listOf(Function(::TokensApi))
}