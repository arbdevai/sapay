package com.arbdev.sapay.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arbdev.sapay.data.model.BillHistoryItem
import com.arbdev.sapay.data.preferences.AppPreferences
import com.arbdev.sapay.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class AuthUiState(
    val phoneInput: String = "",
    val passwordInput: String = "",
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val token: String? = null,
    val history: List<BillHistoryItem> = emptyList(),
    val isLoadingHistory: Boolean = false
)

class AuthViewModel(
    private val repository: AuthRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkSavedSession()
    }

    private fun checkSavedSession() {
        viewModelScope.launch {
            val token = preferences.authToken.first()
            val phone = preferences.userPhone.first()
            if (token.isNotBlank()) {
                _uiState.value = _uiState.value.copy(
                    isLoggedIn = true,
                    token = token,
                    phoneInput = phone
                )
                loadHistory()
            }
        }
    }

    fun onPhoneChanged(value: String) {
        _uiState.value = _uiState.value.copy(phoneInput = value, errorMessage = null)
    }

    fun onPasswordChanged(value: String) {
        _uiState.value = _uiState.value.copy(passwordInput = value, errorMessage = null)
    }

    fun login() {
        val phone = _uiState.value.phoneInput.trim()
        val pass = _uiState.value.passwordInput.trim()

        if (phone.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Nomor HP tidak boleh kosong")
            return
        }
        if (pass.length < 6) {
            _uiState.value = _uiState.value.copy(errorMessage = "Password minimal 6 karakter")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = repository.login(phone, pass)
            result.fold(
                onSuccess = { token ->
                    preferences.saveAuthToken(token, phone)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        token = token,
                        passwordInput = ""
                    )
                    loadHistory()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Login gagal. Periksa nomor HP dan password."
                    )
                }
            )
        }
    }

    fun loadHistory() {
        val token = _uiState.value.token ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingHistory = true)
            val result = repository.getHistory(token)
            result.fold(
                onSuccess = { list ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingHistory = false,
                        history = list
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingHistory = false,
                        errorMessage = error.message ?: "Gagal memuat histori tagihan"
                    )
                }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            preferences.clearAuth()
            _uiState.value = AuthUiState()
        }
    }
}
