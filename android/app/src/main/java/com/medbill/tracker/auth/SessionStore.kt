package com.medbill.tracker.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SessionStore {
    private val _accessToken = MutableStateFlow<String?>(null)
    val accessToken: StateFlow<String?> = _accessToken.asStateFlow()

    fun setToken(token: String?) {
        _accessToken.value = token
    }

    fun clear() {
        _accessToken.value = null
    }
}
