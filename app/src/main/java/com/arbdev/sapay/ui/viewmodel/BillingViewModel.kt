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
    val selectedPackage: RegionalPackage? = null,
    val availablePackages: List<RegionalPackage> = emptyList(),
    val dataToken: String? = null,
    val errorMessage: String? = null,
    val needsReactivation: Boolean = false,
    val isReactivating: Boolean = false,
    val isDataConfirmed: Boolean = false,
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
        _uiState.value = _uiState.value.copy(
            queryInput = newQuery,
            errorMessage = null,
            needsReactivation = false
        )
    }

    fun onConfirmCheckboxChanged(checked: Boolean) {
        _uiState.value = _uiState.value.copy(isDataConfirmed = checked)
    }

    fun onSelectPackage(pkg: RegionalPackage) {
        _uiState.value = _uiState.value.copy(selectedPackage = pkg)
    }

    fun checkBill(query: String? = null) {
        val payload = (query ?: _uiState.value.queryInput).trim()
        if (payload.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Nomor Pelanggan Harus Diisi!")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                needsReactivation = false,
                customerInfo = null,
                isDataConfirmed = false
            )
            val result = repository.checkBill(payload)
            result.fold(
                onSuccess = { response ->
                    val customer = response.customer
                    if (customer != null) {
                        val initialPackage = customer.regionalPackage ?: customer.activeRegionalPackage
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            customerInfo = customer,
                            selectedPackage = initialPackage,
                            dataToken = response.data,
                            errorMessage = null
                        )

                        val idToSave = customer.customerId ?: customer.phoneNumber ?: payload
                        val nameToSave = customer.name ?: ""
                        preferences.saveCustomerInfo(idToSave, nameToSave)

                        // Fetch available packages for this customer
                        fetchAvailablePackages(customer.customerId, customer.mitraId)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = response.message ?: "Customer tidak ditemukan"
                        )
                    }
                },
                onFailure = { error ->
                    val msg = error.message ?: "Gagal memverifikasi pelanggan"
                    if (msg.contains("reaktivasi", ignoreCase = true)) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            needsReactivation = true,
                            errorMessage = msg
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = msg
                        )
                    }
                }
            )
        }
    }

    private fun fetchAvailablePackages(customerId: String?, mitraId: String?) {
        viewModelScope.launch {
            val result = repository.getAvailablePackages(customerId, mitraId)
            result.onSuccess { packages ->
                if (packages.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(availablePackages = packages)
                }
            }
        }
    }

    fun reactivateCustomer() {
        val payload = _uiState.value.queryInput.trim()
        if (payload.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isReactivating = true, errorMessage = null)
            val result = repository.reactivateCustomer(payload)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isReactivating = false,
                        needsReactivation = false,
                        statusMessage = "Akun berhasil direaktivasi."
                    )
                    // Automatically re-check bill as on the web
                    checkBill(payload)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isReactivating = false,
                        errorMessage = error.message ?: "Gagal mereaktivasi akun"
                    )
                }
            )
        }
    }

    fun dismissReactivation() {
        _uiState.value = _uiState.value.copy(needsReactivation = false)
    }

    fun loadPaymentChannels(forceRefresh: Boolean = false) {
        if (!forceRefresh && _uiState.value.paymentChannels.isNotEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
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
        val packageId = _uiState.value.selectedPackage?.id
            ?: customer.regionalPackage?.id
            ?: customer.activeRegionalPackage?.id

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = repository.createPayment(
                customerId = customer.id ?: "",
                regionalPackageId = packageId,
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
                                    errorMessage = error.message ?: "Gagal memuat rincian transaksi"
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
            _uiState.value = _uiState.value.copy(isCheckingStatus = true, errorMessage = null)
            val currentPay = _uiState.value.currentPayment
            val customer = _uiState.value.customerInfo
            val targetTrx = trxCode.ifBlank { currentPay?.trxCode ?: "" }
            val customerId = currentPay?.customerIdString
                ?: customer?.id
                ?: ""
            val customerQuery = customer?.customerId
                ?: customer?.phoneNumber
                ?: _uiState.value.queryInput.trim()

            // 1. Call official check-payment-status API with trx_code and customer_id
            val statusResult = repository.checkPaymentStatus(targetTrx, customerId)
            var settled = statusResult.getOrNull()?.isSettled == true

            // 2. Also check current-payment transaction
            if (!settled && customerId.isNotBlank()) {
                val currentResult = repository.getCurrentPayment(customerId)
                currentResult.onSuccess { active ->
                    if (active.paymentStatus.equals("settle", ignoreCase = true) ||
                        active.paymentStatus.equals("success", ignoreCase = true)
                    ) {
                        settled = true
                        _uiState.value = _uiState.value.copy(currentPayment = active)
                    }
                }
            }

            // 3. Fallback verification via bill check if transaction is closed or settled
            if (!settled && customerQuery.isNotBlank()) {
                val billResult = repository.checkBill(customerQuery)
                billResult.onSuccess { billResp ->
                    val msg = billResp.message?.lowercase() ?: ""
                    if (msg.contains("lunas") || msg.contains("tidak ada tagihan")) {
                        settled = true
                    }
                }
            }

            if (settled) {
                _uiState.value = _uiState.value.copy(
                    isCheckingStatus = false,
                    isPaymentSuccess = true,
                    statusMessage = "Pembayaran Berhasil! Tagihan Anda telah lunas."
                )
            } else {
                val errMsg = statusResult.exceptionOrNull()?.message
                _uiState.value = _uiState.value.copy(
                    isCheckingStatus = false,
                    statusMessage = if (errMsg.isNullOrBlank()) {
                        "Status diperbarui. Pembayaran belum terkonfirmasi, silakan selesaikan pembayaran."
                    } else {
                        errMsg
                    }
                )
            }
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
