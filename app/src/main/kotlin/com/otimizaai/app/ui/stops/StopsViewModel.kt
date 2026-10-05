package com.otimizaai.app.ui.stops

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otimizaai.app.settings.todaySession
import com.otimizaai.domain.model.DeliveryAddress
import com.otimizaai.domain.model.DeliveryStatus
import com.otimizaai.domain.model.DeliveryStop
import com.otimizaai.domain.model.NativeStopId
import com.otimizaai.domain.model.Platform
import com.otimizaai.domain.model.RegisterStopResult
import com.otimizaai.domain.repository.DeliveryStopRepository
import com.otimizaai.domain.util.BrNumber
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StopsViewModel @Inject constructor(
    private val repository: DeliveryStopRepository,
) : ViewModel() {

    private val session = todaySession()

    /** Paradas de hoje, na ordem em que foram cadastradas. */
    val stops: StateFlow<List<DeliveryStop>> = repository.observeBySession(session)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /** Parada que já existe em outra jornada (ex.: falhou ontem) e aguarda decisão. */
    private val _conflict = MutableStateFlow<DeliveryStop?>(null)
    val conflict: StateFlow<DeliveryStop?> = _conflict.asStateFlow()

    /**
     * Valida e grava uma parada digitada. Retorna um erro para mostrar no formulário,
     * ou null se os dados estão ok (o resultado da gravação chega por [message]/[conflict]).
     *
     * O texto digitado é "limpo" aqui (espaços nas pontas), que é a ORIGEM do dado.
     * Depois disso o ID nunca mais é alterado (regra #1).
     */
    fun addStop(platform: Platform?, idText: String, addressText: String, freightText: String): String? {
        if (platform == null) return "Escolha a plataforma."
        val id = idText.trim()
        if (id.isEmpty()) return "Informe o ID do pedido (como aparece na plataforma)."
        val address = addressText.trim()
        if (address.isEmpty()) return "Informe o endereço de entrega."
        val freight = if (freightText.isBlank()) 0L else BrNumber.parseCents(freightText)
        if (freight == null || freight < 0 || freight > Int.MAX_VALUE) return "Valor do frete inválido (ex.: 11,50)."

        val stop = try {
            DeliveryStop.create(
                id = NativeStopId(id),
                platform = platform,
                sessionId = session,
                address = DeliveryAddress(address),
                freightCents = freight.toInt(),
            )
        } catch (e: IllegalArgumentException) {
            return e.message ?: "Dados inválidos."
        }

        viewModelScope.launch {
            when (val result = repository.register(stop)) {
                RegisterStopResult.Inserted -> _message.value = "Parada adicionada."
                RegisterStopResult.AlreadyInSession -> _message.value = "Esse pedido já está na lista de hoje."
                is RegisterStopResult.ExistsInOtherSession -> _conflict.value = result.existing
            }
        }
        return null
    }

    /** REENTREGA confirmada pelo entregador: traz a parada para a jornada de hoje. */
    fun confirmTransfer() {
        val stop = _conflict.value ?: return
        _conflict.value = null
        viewModelScope.launch {
            val moved = repository.transferToSession(stop.key, session)
            _message.value = if (moved) "Parada trazida para hoje." else "Não foi possível mover a parada."
        }
    }

    fun dismissConflict() {
        _conflict.value = null
    }

    fun setStatus(stop: DeliveryStop, status: DeliveryStatus) {
        viewModelScope.launch { repository.updateStatus(stop.key, status) }
    }

    fun consumeMessage() {
        _message.value = null
    }
}
