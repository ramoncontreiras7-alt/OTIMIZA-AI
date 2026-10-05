package com.otimizaai.app.ui.stops

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otimizaai.app.settings.ActiveRouteManager
import com.otimizaai.app.settings.AppSettings
import com.otimizaai.app.settings.SettingsStore
import com.otimizaai.domain.model.DeliveryAddress
import com.otimizaai.domain.model.DeliveryStatus
import com.otimizaai.domain.model.DeliveryStop
import com.otimizaai.domain.model.NativeStopId
import com.otimizaai.domain.model.Platform
import com.otimizaai.domain.model.RegisterStopResult
import com.otimizaai.domain.model.Route
import com.otimizaai.domain.repository.DeliveryStopRepository
import com.otimizaai.domain.util.BrNumber
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StopsViewModel @Inject constructor(
    private val repository: DeliveryStopRepository,
    private val activeRoutes: ActiveRouteManager,
    settingsStore: SettingsStore,
) : ViewModel() {

    val route: StateFlow<Route?> = activeRoutes.active

    val settings: StateFlow<AppSettings> = settingsStore.settings

    /** Paradas da rota aberta, na ordem da rota. */
    val stops: StateFlow<List<DeliveryStop>> = activeRoutes.active.filterNotNull()
        .flatMapLatest { repository.observeBySession(it.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /** Parada que já existe em outra rota (ex.: falhou ontem) e aguarda decisão. */
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
        val session = activeRoutes.active.value?.id ?: return "Aguarde, abrindo a rota..."
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
                RegisterStopResult.AlreadyInSession -> _message.value = "Esse pedido já está nesta rota."
                is RegisterStopResult.ExistsInOtherSession -> _conflict.value = result.existing
            }
        }
        return null
    }

    /** REENTREGA confirmada pelo entregador: traz a parada para a rota aberta. */
    fun confirmTransfer() {
        val stop = _conflict.value ?: return
        val session = activeRoutes.active.value?.id ?: return
        _conflict.value = null
        viewModelScope.launch {
            val moved = repository.transferToSession(stop.key, session)
            _message.value = if (moved) "Parada trazida para esta rota." else "Não foi possível mover a parada."
        }
    }

    fun dismissConflict() {
        _conflict.value = null
    }

    fun setStatus(stop: DeliveryStop, status: DeliveryStatus) {
        viewModelScope.launch { repository.updateStatus(stop.key, status) }
    }

    /** Sobe (-1) ou desce (+1) a parada uma posição na rota. */
    fun move(stop: DeliveryStop, delta: Int) {
        val session = activeRoutes.active.value?.id ?: return
        val list = stops.value.toMutableList()
        val from = list.indexOfFirst { it.key == stop.key }
        val to = from + delta
        if (from < 0 || to < 0 || to >= list.size) return
        list.add(to, list.removeAt(from))
        viewModelScope.launch { repository.reorder(session, list.map { it.key }) }
    }

    fun delete(stop: DeliveryStop) {
        viewModelScope.launch {
            repository.delete(stop.key)
            _message.value = "Parada removida."
        }
    }

    fun bringPending() {
        val session = activeRoutes.active.value?.id ?: return
        viewModelScope.launch {
            val moved = repository.transferPendingFromOtherSessions(session)
            _message.value = if (moved > 0) "$moved parada(s) pendente(s) trazida(s)." else "Nenhuma parada pendente em outras rotas."
        }
    }

    fun consumeMessage() {
        _message.value = null
    }
}
