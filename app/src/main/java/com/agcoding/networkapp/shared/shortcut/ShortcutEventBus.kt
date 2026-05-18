package com.agcoding.networkapp.shared.shortcut

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ShortcutEventBus {
    private val _pendingEvent = MutableStateFlow<ShortcutEvent?>(null)
    val pendingEvent: StateFlow<ShortcutEvent?> = _pendingEvent.asStateFlow()

    fun post(event: ShortcutEvent) {
        _pendingEvent.value = event
    }

    fun consume() {
        _pendingEvent.value = null
    }
}

enum class ShortcutEvent {
    AddSnapshot
}
