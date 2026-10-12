package com.arbdev.sapay

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arbdev.sapay.data.api.ApiClient
import com.arbdev.sapay.data.preferences.AppPreferences
import com.arbdev.sapay.data.repository.AuthRepository
import com.arbdev.sapay.data.repository.BillingRepository
import com.arbdev.sapay.data.repository.UpdateRepository
import com.arbdev.sapay.ui.screens.MainScreen
import com.arbdev.sapay.ui.theme.SaPayTheme
import com.arbdev.sapay.ui.util.StatusBarNotificationUtil
import com.arbdev.sapay.ui.viewmodel.AuthViewModel
import com.arbdev.sapay.ui.viewmodel.BillingViewModel
import com.arbdev.sapay.ui.viewmodel.UpdateViewModel
import com.arbdev.sapay.worker.BillReminderWorker

class MainActivity : ComponentActivity() {

    private var billingViewModelRef: BillingViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        StatusBarNotificationUtil.createNotificationChannels(applicationContext)

        // Jadwalkan WorkManager periodic bill reminder di background
        BillReminderWorker.schedulePeriodicReminder(applicationContext)

        val preferences = AppPreferences(applicationContext)
        val billingRepository = BillingRepository(ApiClient.starliteApi)
        val authRepository = AuthRepository(ApiClient.starliteApi)
        val updateRepository = UpdateRepository(ApiClient.gitHubApi)

        val initialCustomerId = extractCustomerIdFromIntent(intent)

        setContent {
            SaPayTheme {
                val billingViewModel: BillingViewModel = viewModel {
                    BillingViewModel(billingRepository, preferences).apply {
                        if (!initialCustomerId.isNullOrBlank()) {
                            onQueryChanged(initialCustomerId)
                            checkBill(initialCustomerId)
                        }
                    }
                }
                billingViewModelRef = billingViewModel

                val authViewModel: AuthViewModel = viewModel {
                    AuthViewModel(authRepository, preferences)
                }
                val updateViewModel: UpdateViewModel = viewModel {
                    UpdateViewModel(updateRepository)
                }

                MainScreen(
                    billingViewModel = billingViewModel,
                    authViewModel = authViewModel,
                    updateViewModel = updateViewModel
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val targetCustomerId = extractCustomerIdFromIntent(intent)
        if (!targetCustomerId.isNullOrBlank()) {
            billingViewModelRef?.apply {
                onQueryChanged(targetCustomerId)
                checkBill(targetCustomerId)
            }
        }
    }

    private fun extractCustomerIdFromIntent(intent: Intent?): String? {
        if (intent == null) return null

        // 1. Ekstraksi dari extra intent langsung (misal klik status bar notifikasi)
        val extraId = intent.getStringExtra("extra_customer_id")
        if (!extraId.isNullOrBlank()) return extraId.trim()

        // 2. Ekstraksi dari Deep Link (URL yang diklik dari WhatsApp atau browser)
        val uri: Uri = intent.data ?: return null

        val queryParams = listOf("customer_id", "id", "nomor", "phone", "nohp", "payload", "cust_id")
        for (param in queryParams) {
            val value = uri.getQueryParameter(param)
            if (!value.isNullOrBlank()) return value.trim()
        }

        val lastSegment = uri.lastPathSegment
        if (!lastSegment.isNullOrBlank()) {
            val ignored = setOf("pay", "bill", "billing", "microsite", "login", "index", "home", "")
            if (!ignored.contains(lastSegment.lowercase()) && lastSegment.matches(Regex("^[a-zA-Z0-9_-]{3,25}$"))) {
                return lastSegment.trim()
            }
        }

        return null
    }
}
