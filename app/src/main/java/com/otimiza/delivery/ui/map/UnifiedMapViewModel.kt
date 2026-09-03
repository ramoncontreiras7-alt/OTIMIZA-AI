package com.otimiza.delivery.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otimiza.delivery.domain.model.DeliveryStop
import com.otimiza.delivery.domain.model.Platform
import com.otimiza.delivery.domain.repository.DeliveryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class UnifiedMapViewModel @Inject constructor(
    private val repository: DeliveryRepository
) : ViewModel() {

    private val _sessionId = MutableStateFlow<String?>(null)

    val stopsByPlatform: StateFlow<Map<Platform, List<DeliveryStop>>> =
        _sessionId
            .flatMapLatest { id ->
                id?.let { repository.observeStopsBySession(it) } ?: flowOf(emptyList())
            }
            .map { list -> list.groupBy { it.platform } }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    fun setSession(sessionId: String) {
        _sessionId.value = sessionId
    }
}
