package com.example.shortformvideofeed.core.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NetworkSimulationState {

    private val _badNetworkEnabled = MutableStateFlow(false)
    val badNetworkEnabled: StateFlow<Boolean> = _badNetworkEnabled.asStateFlow()

    fun setBadNetworkEnabled(enabled: Boolean) {
        _badNetworkEnabled.value = enabled
    }

    fun isBadNetworkEnabled(): Boolean = _badNetworkEnabled.value
}
