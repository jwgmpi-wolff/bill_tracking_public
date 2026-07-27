package com.medbill.tracker.vm

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medbill.tracker.auth.AuthManager
import com.medbill.tracker.auth.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthState(
    val isAuthenticated: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null
)

class AuthViewModel(private val authManager: AuthManager) : ViewModel() {
    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    init {
        restoreSession()
    }

    private fun restoreSession() {
        viewModelScope.launch {
            runCatching {
                val result = authManager.restoreExistingSession()
                if (result != null && !result.accessToken.isNullOrBlank()) {
                    SessionStore.setToken(result.accessToken)
                    _state.value = AuthState(isAuthenticated = true, loading = false)
                }
            }
        }
    }

    fun signIn(activity: Activity) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching {
                val result = authManager.signIn(activity)
                val refreshed = authManager.restoreExistingSession()
                val token = refreshed?.accessToken ?: result.accessToken
                if (token.isNullOrBlank()) {
                    throw IllegalStateException("Authentication succeeded but API access token was not issued for scope ${com.medbill.tracker.BuildConfig.ENTRA_API_SCOPE}.")
                }

                SessionStore.setToken(token)
                _state.value = AuthState(isAuthenticated = true, loading = false)
            }.onFailure { throwable ->
                val message = throwable.message ?: "Authentication failed"
                val alreadySignedIn = message.contains("already signed in", ignoreCase = true)
                if (alreadySignedIn) {
                    runCatching {
                        val restored = authManager.restoreExistingSession()
                        if (restored != null && !restored.accessToken.isNullOrBlank()) {
                            SessionStore.setToken(restored.accessToken)
                            _state.value = AuthState(isAuthenticated = true, loading = false)
                            return@onFailure
                        }
                    }
                }
                _state.value = AuthState(
                    isAuthenticated = false,
                    loading = false,
                    error = mapAuthError(message)
                )
            }
        }
    }

    fun signInWithDifferentAccount(activity: Activity) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching {
                authManager.signOut()
                SessionStore.clear()
                val result = authManager.signIn(activity)
                val refreshed = authManager.restoreExistingSession()
                val token = refreshed?.accessToken ?: result.accessToken
                if (token.isNullOrBlank()) {
                    throw IllegalStateException("Authentication succeeded but API access token was not issued for scope ${com.medbill.tracker.BuildConfig.ENTRA_API_SCOPE}.")
                }

                SessionStore.setToken(token)
                _state.value = AuthState(isAuthenticated = true, loading = false)
            }.onFailure {
                _state.value = AuthState(isAuthenticated = false, loading = false, error = mapAuthError(it.message ?: "Authentication failed"))
            }
        }
    }

    private fun mapAuthError(message: String): String {
        if (message.contains("AADSTS50020", ignoreCase = true)) {
            return "This tenant only allows organization accounts. Use your tenant UPN (for example, kellywolff@wolffentp.net), then tap Use different account and sign in again."
        }

        return message
    }

    fun signOut() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            runCatching {
                authManager.signOut()
                SessionStore.clear()
                _state.value = AuthState(isAuthenticated = false, loading = false)
            }.onFailure {
                _state.value = _state.value.copy(loading = false, error = it.message)
            }
        }
    }
}
