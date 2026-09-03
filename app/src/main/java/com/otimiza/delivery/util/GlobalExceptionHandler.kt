package com.otimiza.delivery.util

import com.otimiza.delivery.data.remote.VrpEngineException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

sealed interface Failure {
    data class Network(val message: String) : Failure
    data class Server(val message: String) : Failure
    data class Persistence(val message: String) : Failure
    data class Unknown(val message: String) : Failure
}

object GlobalExceptionHandler {

    fun Throwable.toFailure(): Failure = when (this) {
        is UnknownHostException -> Failure.Network("Sem conexão com a rede. Verifique e tente novamente.")
        is SocketTimeoutException -> Failure.Network("Tempo de resposta excedido. Tente novamente.")
        is VrpEngineException -> Failure.Server(message ?: "Motor de roteamento indisponível.")
        else -> Failure.Unknown(message ?: "Erro inesperado.")
    }
}
