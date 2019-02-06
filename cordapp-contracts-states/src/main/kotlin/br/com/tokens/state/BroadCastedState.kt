package br.com.tokens.state

import net.corda.core.contracts.LinearState

interface BroadCastedState<out T>: LinearState {
    val broadcastedValue: T
}