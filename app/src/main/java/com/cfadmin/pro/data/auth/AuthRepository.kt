package com.cfadmin.pro.data.auth

import com.cfadmin.pro.data.api.CloudflareApi
import com.cfadmin.pro.data.api.CloudflareClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Loading : AuthState()
    object LoggedOut : AuthState()
    data class LoggedIn(val token: String) : AuthState()
}

class AuthRepository(private val tokenStore: TokenStore) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private var _api: CloudflareApi? = null

    val api: CloudflareApi
        get() = _api ?: throw IllegalStateException("No hay API client. Inicia sesion primero.")

    init {
        var stored: String? = null
        try {
            stored = tokenStore.getToken()
        } catch (e: Exception) {
            try { tokenStore.clear() } catch (_: Exception) {}
        }

        if (stored != null && stored.isNotBlank()) {
            _api = CloudflareClient.create(stored)
            _state.value = AuthState.LoggedIn(stored)
            scope.launch {
                try {
                    val r = _api?.verifyToken()
                    if (r != null && !r.success) logout()
                } catch (e: Exception) {
                    // Sin red o error temporal: mantenemos sesion
                }
            }
        } else {
            _state.value = AuthState.LoggedOut
        }
    }

    suspend fun login(rawToken: String): Result<Unit> {
        val token = rawToken.trim()
        if (token.isEmpty()) {
            return Result.failure(IllegalArgumentException("Token vacio"))
        }
        return try {
            val newApi = CloudflareClient.create(token)
            val response = newApi.verifyToken()
            if (!response.success) {
                val msg = response.errors.firstOrNull()?.message
                    ?: "Token rechazado por Cloudflare"
                return Result.failure(Exception(msg))
            }
            _api = newApi
            tokenStore.saveToken(token)
            _state.value = AuthState.LoggedIn(token)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        tokenStore.clear()
        _api = null
        _state.value = AuthState.LoggedOut
    }

    fun saveAccount(id: String, name: String) {
        tokenStore.saveAccountId(id)
        tokenStore.saveAccountName(name)
    }

    fun getAccountId(): String? = tokenStore.getAccountId()
    fun getAccountName(): String? = tokenStore.getAccountName()
}
