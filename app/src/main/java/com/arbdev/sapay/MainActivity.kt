package com.arbdev.sapay

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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        StatusBarNotificationUtil.createNotificationChannels(applicationContext)

        val preferences = AppPreferences(applicationContext)
        val billingRepository = BillingRepository(ApiClient.starliteApi)
        val authRepository = AuthRepository(ApiClient.starliteApi)
        val updateRepository = UpdateRepository(ApiClient.gitHubApi)

        val initialCustomerId = intent?.getStringExtra("extra_customer_id")

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
}
