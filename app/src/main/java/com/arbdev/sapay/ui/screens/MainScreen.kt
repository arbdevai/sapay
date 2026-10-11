package com.arbdev.sapay.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.arbdev.sapay.ui.theme.CardSurface
import com.arbdev.sapay.ui.theme.PrimaryBlue
import com.arbdev.sapay.ui.theme.TextSecondary
import com.arbdev.sapay.ui.viewmodel.AuthViewModel
import com.arbdev.sapay.ui.viewmodel.BillingViewModel
import com.arbdev.sapay.ui.viewmodel.UpdateViewModel

enum class MainTab {
    BILLING, ACCOUNT
}

enum class BillingSubScreen {
    DASHBOARD, CHANNELS, DETAIL
}

@Composable
fun MainScreen(
    billingViewModel: BillingViewModel,
    authViewModel: AuthViewModel,
    updateViewModel: UpdateViewModel
) {
    val context = LocalContext.current
    val billingState by billingViewModel.uiState.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    val updateState by updateViewModel.uiState.collectAsState()

    var selectedTab by remember { mutableStateOf(MainTab.BILLING) }
    var currentSubScreen by remember { mutableStateOf(BillingSubScreen.DASHBOARD) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(updateState.infoMessage) {
        val msg = updateState.infoMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            updateViewModel.clearInfoMessage()
        }
    }

    LaunchedEffect(billingState.statusMessage) {
        val msg = billingState.statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (currentSubScreen == BillingSubScreen.DASHBOARD || selectedTab == MainTab.ACCOUNT) {
                NavigationBar(
                    containerColor = CardSurface
                ) {
                    NavigationBarItem(
                        selected = selectedTab == MainTab.BILLING,
                        onClick = {
                            selectedTab = MainTab.BILLING
                            currentSubScreen = BillingSubScreen.DASHBOARD
                        },
                        icon = {
                            Icon(Icons.Default.ReceiptLong, contentDescription = "Cek Tagihan")
                        },
                        label = {
                            Text(
                                "Cek Tagihan",
                                fontWeight = if (selectedTab == MainTab.BILLING) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryBlue,
                            selectedTextColor = PrimaryBlue,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )

                    NavigationBarItem(
                        selected = selectedTab == MainTab.ACCOUNT,
                        onClick = {
                            selectedTab = MainTab.ACCOUNT
                            if (authState.isLoggedIn && authState.profile == null) {
                                authViewModel.loadProfileAndHistory()
                            }
                        },
                        icon = {
                            Icon(Icons.Default.AccountCircle, contentDescription = "Akun Starlite")
                        },
                        label = {
                            Text(
                                "Akun Starlite",
                                fontWeight = if (selectedTab == MainTab.ACCOUNT) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryBlue,
                            selectedTextColor = PrimaryBlue,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Surface(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                MainTab.BILLING -> {
                    when (currentSubScreen) {
                        BillingSubScreen.DASHBOARD -> {
                            QuickBillScreen(
                                state = billingState,
                                onQueryChanged = billingViewModel::onQueryChanged,
                                onCheckBill = { billingViewModel.checkBill() },
                                onConfirmCheckboxChanged = billingViewModel::onConfirmCheckboxChanged,
                                onSelectPackage = billingViewModel::onSelectPackage,
                                onProceedToPayment = {
                                    billingViewModel.loadPaymentChannels()
                                    currentSubScreen = BillingSubScreen.CHANNELS
                                },
                                onViewCurrentPayment = {
                                    currentSubScreen = BillingSubScreen.DETAIL
                                },
                                onReactivateClick = { billingViewModel.reactivateCustomer() },
                                onDismissReactivation = { billingViewModel.dismissReactivation() }
                            )
                        }
                        BillingSubScreen.CHANNELS -> {
                            PaymentChannelScreen(
                                state = billingState,
                                onBackClick = { currentSubScreen = BillingSubScreen.DASHBOARD },
                                onSelectChannel = billingViewModel::selectChannel,
                                onConfirmPayment = {
                                    billingViewModel.processPayment(
                                        onReadyToPay = {
                                            currentSubScreen = BillingSubScreen.DETAIL
                                        }
                                    )
                                }
                            )
                        }
                        BillingSubScreen.DETAIL -> {
                            PaymentDetailScreen(
                                state = billingState,
                                onBackClick = { currentSubScreen = BillingSubScreen.DASHBOARD },
                                onCheckStatus = billingViewModel::checkPaymentStatus
                            )
                        }
                    }
                }
                MainTab.ACCOUNT -> {
                    AccountScreen(
                        state = authState,
                        onPhoneChanged = authViewModel::onPhoneChanged,
                        onPasswordChanged = authViewModel::onPasswordChanged,
                        onLoginClick = authViewModel::login,
                        onLogoutClick = authViewModel::logout,
                        onRefresh = { authViewModel.loadProfileAndHistory() },
                        onPayFromHistory = { historyItem ->
                            val customerId = authState.profile?.customerId
                                ?: authState.phoneInput
                            if (customerId.isNotBlank()) {
                                billingViewModel.onQueryChanged(customerId)
                                billingViewModel.checkBill(customerId)
                                selectedTab = MainTab.BILLING
                                currentSubScreen = BillingSubScreen.DASHBOARD
                            }
                        },
                        onRenewPackageClick = { userProfile ->
                            val customerId = userProfile.customerId
                                ?: userProfile.phoneNumber
                                ?: ""
                            if (customerId.isNotBlank()) {
                                billingViewModel.onQueryChanged(customerId)
                                billingViewModel.checkBill(customerId)
                                selectedTab = MainTab.BILLING
                                currentSubScreen = BillingSubScreen.DASHBOARD
                            }
                        },
                        onCheckUpdateClick = { updateViewModel.checkForUpdate(manual = true) }
                    )
                }
            }
        }
    }

    // In-App Update Changelog Modal
    if (updateState.showChangelogDialog && updateState.latestRelease != null) {
        ChangelogModal(
            release = updateState.latestRelease!!,
            onDismiss = { updateViewModel.showChangelog(false) },
            onUpdateClick = { updateViewModel.startDownloadAndInstall(context) }
        )
    }
}
