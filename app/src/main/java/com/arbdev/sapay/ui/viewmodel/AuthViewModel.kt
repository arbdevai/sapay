package com.arbdev.sapay.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arbdev.sapay.data.model.BillHistoryItem
import com.arbdev.sapay.data.model.CustomerInfo
import com.arbdev.sapay.data.model.RegionalPackage
import com.arbdev.sapay.data.model.UserProfile
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
    val statusMessage: String? = null,
    val token: String? = null,
    val profile: UserProfile? = null,
    val isLoadingProfile: Boolean = false,
    val history: List<BillHistoryItem> = emptyList(),
    val isLoadingHistory: Boolean = false,
    val isResettingPassword: Boolean = false,
    val showForgotPasswordDialog: Boolean = false,
    val showLogoutConfirmDialog: Boolean = false
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
                loadProfileAndHistory(token)
            }
        }
    }

    fun onPhoneChanged(value: String) {
        _uiState.value = _uiState.value.copy(phoneInput = value, errorMessage = null)
    }

    fun onPasswordChanged(value: String) {
        _uiState.value = _uiState.value.copy(passwordInput = value, errorMessage = null)
    }

    fun showLogoutConfirm(show: Boolean) {
        _uiState.value = _uiState.value.copy(showLogoutConfirmDialog = show)
    }

    fun showForgotPassword(show: Boolean) {
        _uiState.value = _uiState.value.copy(showForgotPasswordDialog = show, errorMessage = null)
    }

    fun clearStatusMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
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
                        passwordInput = "",
                        statusMessage = "Login berhasil."
                    )
                    loadProfileAndHistory(token)
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

    fun forgotPassword(phone: String) {
        val targetPhone = phone.trim().ifBlank { _uiState.value.phoneInput.trim() }
        if (targetPhone.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Nomor HP harus diisi.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isResettingPassword = true, errorMessage = null)
            val result = repository.forgotPassword(targetPhone)
            result.fold(
                onSuccess = { msg ->
                    _uiState.value = _uiState.value.copy(
                        isResettingPassword = false,
                        showForgotPasswordDialog = false,
                        statusMessage = msg
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isResettingPassword = false,
                        errorMessage = error.message ?: "Gagal mengirim link reset password."
                    )
                }
            )
        }
    }

    fun loadProfileAndHistory(token: String? = null) {
        val activeToken = token ?: _uiState.value.token ?: return
        loadProfile(activeToken)
        loadHistory(activeToken)
    }

    fun loadProfile(token: String? = null) {
        val activeToken = token ?: _uiState.value.token ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingProfile = true)
            val result = repository.getProfile(activeToken)
            result.fold(
                onSuccess = { profileData ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingProfile = false,
                        profile = profileData
                    )
                    // Also save customer ID for quick bill sync
                    if (!profileData.customerId.isNullOrBlank()) {
                        preferences.saveCustomerInfo(profileData.customerId, profileData.name ?: "")
                    }
                },
                onFailure = { error ->
                    val errorMsg = error.message ?: "Gagal memuat profil"
                    if (isAuthExpiredError(errorMsg)) {
                        logout()
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "Sesi telah berakhir. Silakan login kembali."
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoadingProfile = false,
                            errorMessage = if (_uiState.value.profile == null) errorMsg else null
                        )
                    }
                }
            )
        }
    }

    fun loadHistory(token: String? = null) {
        val activeToken = token ?: _uiState.value.token ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingHistory = true)
            val result = repository.getHistory(activeToken, page = 1, pageSize = 20)
            result.fold(
                onSuccess = { list ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingHistory = false,
                        history = list
                    )
                },
                onFailure = { error ->
                    val errorMsg = error.message ?: "Gagal memuat riwayat transaksi"
                    if (isAuthExpiredError(errorMsg)) {
                        logout()
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "Sesi telah berakhir. Silakan login kembali."
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoadingHistory = false,
                            errorMessage = errorMsg
                        )
                    }
                }
            )
        }
    }

    private fun isAuthExpiredError(msg: String): Boolean {
        val lower = msg.lowercase()
        return lower.contains("jwt malformed") ||
                lower.contains("jwt expired") ||
                lower.contains("unauthorized") ||
                lower.contains("forbidden")
    }

    fun logout() {
        viewModelScope.launch {
            val currentToken = _uiState.value.token
            if (!currentToken.isNullOrBlank()) {
                repository.logout(currentToken)
            }
            preferences.clearAuth()
            _uiState.value = AuthUiState(
                statusMessage = "Anda telah berhasil keluar dari akun."
            )
        }
    }
}
