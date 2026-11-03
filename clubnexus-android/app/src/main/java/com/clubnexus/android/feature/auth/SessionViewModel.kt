package com.clubnexus.android.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.clubnexus.android.BuildConfig
import com.clubnexus.android.data.auth.Session
import com.clubnexus.android.data.auth.SessionStore
import com.clubnexus.android.data.model.LoginRequest
import com.clubnexus.android.data.model.RegisterRequest
import com.clubnexus.android.data.network.ApiClient
import com.clubnexus.android.data.network.ClubNexusApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException

class SessionViewModel(private val store: SessionStore) : ViewModel() {
    private val api: ClubNexusApi = ApiClient.create(store)
    val session: StateFlow<Session?> = store.session
    var restoring by mutableStateOf(true)
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    init {
        if (BuildConfig.API_BASE_URL.isBlank()) {
            restoring = false
        } else {
            viewModelScope.launch {
                val token = store.token()
                if (token != null) {
                    try {
                        store.updateUser(api.me())
                    } catch (e: Exception) {
                        if (e !is HttpException || e.code() != 401) Unit
                    }
                }
                restoring = false
            }
        }
    }

    fun login(email: String, password: String) = authenticate {
        api.login(LoginRequest(email.trim(), password))
    }

    fun register(name: String, email: String, password: String, department: String, position: String) = authenticate {
        api.register(RegisterRequest(name.trim(), email.trim().lowercase(), password, department.trim(), position.trim()))
    }

    private fun authenticate(request: suspend () -> com.clubnexus.android.data.model.AuthResponse) {
        if (BuildConfig.API_BASE_URL.isBlank()) {
            error = "Set the production API URL before signing in."
            return
        }
        viewModelScope.launch {
            busy = true
            error = null
            try {
                val result = request()
                store.save(result.token, result.user)
            } catch (e: Exception) {
                error = e.toUserMessage()
            } finally {
                busy = false
            }
        }
    }

    fun clearError() { error = null }
    fun logout() = store.clear()

    private fun Exception.toUserMessage(): String {
        if (this is HttpException) {
            val raw = response()?.errorBody()?.string()
            if (!raw.isNullOrBlank()) {
                try {
                    val json = JSONObject(raw)
                    json.optString("message").takeIf { it.isNotBlank() }?.let { return it }
                    val errors = json.optJSONObject("errors")?.optJSONObject("fieldErrors")
                    errors?.keys()?.asSequence()?.mapNotNull { errors.optJSONArray(it)?.optString(0) }?.firstOrNull()?.let { return it }
                } catch (_: Exception) { }
            }
            return "Request failed (${code()})"
        }
        return localizedMessage ?: "Unable to connect to ClubNexus. Check the API URL and your connection."
    }
}
