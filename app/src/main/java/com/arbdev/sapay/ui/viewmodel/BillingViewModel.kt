package com.arbdev.sapay.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arbdev.sapay.data.model.*
import com.arbdev.sapay.data.preferences.AppPreferences
import com.arbdev.sapay.data.repository.BillingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class BillingUiState(
    val queryInput: String = "",
    val isLoading: Boolean = false,
    val customerInfo: CustomerInfo? = null,
    val dataToken: String? = null,
    val errorMessage: String? = null,
    val paymentChannels: List<PaymentChannelGroup> = emptyList(),
    val selectedChannel: PaymentChannelItem? = null,
    val currentPayment: CurrentPaymentResult? = null,
    val isCheckingStatus: Boolean = false,
    val isPaymentSuccess: Boolean = false,
    val statusMessage: String? = null
)

class BillingViewModel(
    private val repository: BillingRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(BillingUiState())
    val uiState: StateFlow<BillingUiState> = _uiState.asStateFlow()

    init {
        loadSavedCustomer()
    }

    private fun loadSavedCustomer() {
        viewModelScope.launch {
            val savedId = preferences.savedCustomerId.first()
            if (savedId.isNotBlank()) {
                _uiState.value = _uiState.value.copy(queryInput = savedId)
                checkBill(savedId)
            }
        }
    }

    fun onQueryChanged(newQuery: String) {
        _uiState.value = _uiState.value.copy(queryInput = newQuery, errorMessage = null)
    }

    fun checkBill(query: String? = null) {
        val payload = (query ?: _uiState.value.queryInput).trim()
        if (payload.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Masukkan Nomor Pelanggan atau Nomor HP")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, customerInfo = null)
            val result = repository.checkBill(payload)
            result.fold(
                onSuccess = { response ->
                    val customer = response.customer
                    if (customer != null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            customerInfo = customer,
                            dataToken = response.data,
                            errorMessage = null
                        )
                        // Save to preferences for quick bill on next open
                        val idToSave = customer.customerId ?: customer.phoneNumber ?: payload
                        val nameToSave = customer.name ?: ""
                        preferences.saveCustomerInfo(idToSave, nameToSave)

                        // Check if there is an active current payment
                        if (!customer.id.isNullOrBlank()) {
                            checkExistingPayment(customer.id)
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = response.message ?: "Data pelanggan tidak ditemukan"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Gagal memverifikasi pelanggan"
                    )
                }
            )
        }
    }

    private fun checkExistingPayment(customerId: String) {
        viewModelScope.launch {
            val result = repository.getCurrentPayment(customerId)
            result.onSuccess { activePayment ->
                if (activePayment.paymentStatus == "settle") {
                    _uiState.value = _uiState.value.copy(
                        currentPayment = activePayment,
                        isPaymentSuccess = true,
                        statusMessage = "Tagihan sudah lunas."
                    )
                } else if (!activePayment.trxCode.isNullOrBlank()) {
                    _uiState.value = _uiState.value.copy(
                        currentPayment = activePayment
                    )
                }
            }
        }
    }

    fun loadPaymentChannels() {
        if (_uiState.value.paymentChannels.isNotEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.getPaymentChannels()
            result.fold(
                onSuccess = { groups ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        paymentChannels = groups
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Gagal memuat metode pembayaran"
                    )
                }
            )
        }
    }

    fun selectChannel(channel: PaymentChannelItem) {
        _uiState.value = _uiState.value.copy(selectedChannel = channel)
    }

    fun processPayment(onReadyToPay: () -> Unit) {
        val customer = _uiState.value.customerInfo ?: return
        val channel = _uiState.value.selectedChannel ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = repository.createPayment(
                customerId = customer.id ?: "",
                regionalPackageId = customer.regionalPackage?.id,
                channelId = channel.id
            )

            result.fold(
                onSuccess = { response ->
                    if (response.message == "Tagihan sudah lunas.") {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isPaymentSuccess = true,
                            statusMessage = "Tagihan sudah lunas."
                        )
                    } else {
                        // Fetch active payment transaction
                        val currResult = repository.getCurrentPayment(customer.id ?: "")
                        currResult.fold(
                            onSuccess = { paymentData ->
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    currentPayment = paymentData
                                )
                                onReadyToPay()
                            },
                            onFailure = { error ->
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    errorMessage = error.message ?: "Gagal memuat rincian transaksi pembayaran"
                                )
                            }
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Gagal memproses metode pembayaran"
                    )
                }
            )
        }
    }

    fun checkPaymentStatus(trxCode: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCheckingStatus = true)
            val result = repository.checkPaymentStatus(trxCode)
            result.fold(
                onSuccess = {
                    // Refresh current payment
                    val customerId = _uiState.value.customerInfo?.id
                    if (!customerId.isNullOrBlank()) {
                        val refreshed = repository.getCurrentPayment(customerId)
                        refreshed.onSuccess { active ->
                            if (active.paymentStatus == "settle") {
                                _uiState.value = _uiState.value.copy(
                                    isCheckingStatus = false,
                                    isPaymentSuccess = true,
                                    currentPayment = active,
                                    statusMessage = "Pembayaran Berhasil! Tagihan telah lunas."
                                )
                                return@launch
                            }
                        }
                    }
                    _uiState.value = _uiState.value.copy(
                        isCheckingStatus = false,
                        statusMessage = "Status diperbarui. Menunggu konfirmasi bank/sistem."
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isCheckingStatus = false,
                        errorMessage = error.message ?: "Belum ada konfirmasi pembayaran baru"
                    )
                }
            )
        }
    }

    fun resetPaymentFlow() {
        _uiState.value = _uiState.value.copy(
            selectedChannel = null,
            currentPayment = null,
            isPaymentSuccess = false,
            errorMessage = null,
            statusMessage = null
        )
    }
}
