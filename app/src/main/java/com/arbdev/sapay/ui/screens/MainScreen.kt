package com.arbdev.sapay.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.arbdev.sapay.ui.components.InAppNotificationBanner
import com.arbdev.sapay.ui.components.InAppNotificationData
import com.arbdev.sapay.ui.components.NotificationType
import com.arbdev.sapay.ui.theme.*
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

    // System Back Gesture handling: returns to dashboard if in subscreen
    BackHandler(enabled = currentSubScreen != BillingSubScreen.DASHBOARD) {
        currentSubScreen = BillingSubScreen.DASHBOARD
    }

    var currentNotification by remember { mutableStateOf<InAppNotificationData?>(null) }

    // Always automatically check for update & reload fresh banners on app resume
    LifecycleResumeEffect(Unit) {
        updateViewModel.checkForUpdate(manual = false)
        billingViewModel.loadBanners()
        onPauseOrDispose { }
    }

    LaunchedEffect(updateState.infoMessage) {
        val msg = updateState.infoMessage
        if (!msg.isNullOrBlank()) {
            val type = if (msg.contains("gagal", ignoreCase = true) || msg.contains("error", ignoreCase = true)) {
                NotificationType.ERROR
            } else if (msg.contains("terbaru", ignoreCase = true) || msg.contains("berhasil", ignoreCase = true)) {
                NotificationType.SUCCESS
            } else {
                NotificationType.INFO
            }
            currentNotification = InAppNotificationData(message = msg, type = type)
            updateViewModel.clearInfoMessage()
        }
    }

    LaunchedEffect(billingState.statusMessage) {
        val msg = billingState.statusMessage
        if (!msg.isNullOrBlank()) {
            val type = if (msg.contains("berhasil", ignoreCase = true) || msg.contains("lunas", ignoreCase = true)) {
                NotificationType.SUCCESS
            } else if (msg.contains("gagal", ignoreCase = true) || msg.contains("kesalahan", ignoreCase = true) ||
                msg.contains("pemeliharaan", ignoreCase = true) || msg.contains("maintenance", ignoreCase = true)) {
                NotificationType.ERROR
            } else {
                NotificationType.INFO
            }
            currentNotification = InAppNotificationData(message = msg, type = type)
        }
    }

    LaunchedEffect(authState.statusMessage) {
        val msg = authState.statusMessage
        if (!msg.isNullOrBlank()) {
            val type = if (msg.contains("berhasil", ignoreCase = true) || msg.contains("sukses", ignoreCase = true)) {
                NotificationType.SUCCESS
            } else if (msg.contains("gagal", ignoreCase = true) || msg.contains("salah", ignoreCase = true) ||
                msg.contains("tidak cocok", ignoreCase = true)) {
                NotificationType.ERROR
            } else {
                NotificationType.INFO
            }
            currentNotification = InAppNotificationData(message = msg, type = type)
            authViewModel.clearStatusMessage()
        }
    }

    Scaffold(
        containerColor = BackgroundLight
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val showNavBar = currentSubScreen == BillingSubScreen.DASHBOARD || selectedTab == MainTab.ACCOUNT

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (showNavBar) 84.dp else 0.dp)
            ) {
                when (selectedTab) {
                MainTab.BILLING -> {
                    when (currentSubScreen) {
                        BillingSubScreen.DASHBOARD -> {
                            QuickBillScreen(
                                state = billingState,
                                updateAvailable = updateState.updateAvailable,
                                latestReleaseTag = updateState.latestRelease?.tagName,
                                onUpdateClick = { updateViewModel.showChangelog(true) },
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
                                onDismissReactivation = { billingViewModel.dismissReactivation() },
                                onNavigateToAccount = {
                                    selectedTab = MainTab.ACCOUNT
                                    if (authState.isLoggedIn && authState.profile == null) {
                                        authViewModel.loadProfileAndHistory()
                                    }
                                }
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
                                },
                                onRetry = { billingViewModel.loadPaymentChannels(forceRefresh = true) }
                            )
                        }
                        BillingSubScreen.DETAIL -> {
                            PaymentDetailScreen(
                                state = billingState,
                                onBackClick = { currentSubScreen = BillingSubScreen.DASHBOARD },
                                onCheckStatus = billingViewModel::checkPaymentStatus,
                                onSaveStoreConfig = billingViewModel::saveStoreConfig,
                                onShowNotification = { msg, type ->
                                    currentNotification = InAppNotificationData(message = msg, type = type)
                                }
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
                        onLogoutClick = {
                            authViewModel.showLogoutConfirm(false)
                            authViewModel.logout()
                        },
                        onRequestLogout = { authViewModel.showLogoutConfirm(true) },
                        onDismissLogout = { authViewModel.showLogoutConfirm(false) },
                        onRequestForgotPassword = { authViewModel.showForgotPassword(true) },
                        onDismissForgotPassword = { authViewModel.showForgotPassword(false) },
                        onSendResetPassword = { phone -> authViewModel.forgotPassword(phone) },
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
                        onCheckUpdateClick = { updateViewModel.checkForUpdate(manual = true) },
                        storeConfig = billingState.storeConfig,
                        onSaveStoreConfig = billingViewModel::saveStoreConfig,
                        onShowNotification = { msg, type ->
                            currentNotification = InAppNotificationData(message = msg, type = type)
                        }
                    )
                }
            }
        }

        // Floating Capsule Navigation Bar (Animated Pill)
        if (showNavBar) {
            FloatingCapsuleNavBar(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    selectedTab = tab
                    if (tab == MainTab.BILLING) {
                        currentSubScreen = BillingSubScreen.DASHBOARD
                    } else if (tab == MainTab.ACCOUNT) {
                        if (authState.isLoggedIn && authState.profile == null) {
                            authViewModel.loadProfileAndHistory()
                        }
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // Custom In-App Floating Notification Banner (Tanpa Notif Bawaan OS)
        InAppNotificationBanner(
            notification = currentNotification,
            onDismiss = { currentNotification = null },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        )
    }
    }

    LaunchedEffect(updateState.showChangelogDialog) {
        if (updateState.showChangelogDialog) {
            updateViewModel.syncExistingDownload(context)
        }
    }

    // In-App Update Changelog Modal
    if (updateState.showChangelogDialog && updateState.latestRelease != null) {
        ChangelogModal(
            release = updateState.latestRelease!!,
            isDownloading = updateState.isDownloading,
            downloadProgress = updateState.downloadProgress,
            isDownloaded = updateState.isDownloaded,
            downloadError = updateState.downloadError,
            onDismiss = { updateViewModel.showChangelog(false) },
            onUpdateClick = { updateViewModel.startDownloadAndInstall(context) }
        )
    }
}

@Composable
fun FloatingCapsuleNavBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(horizontal = 24.dp, vertical = 18.dp)
            .shadow(
                elevation = 14.dp,
                shape = CircleShape,
                spotColor = PrimaryBlue.copy(alpha = 0.3f),
                ambientColor = PrimaryBlue.copy(alpha = 0.12f)
            ),
        shape = CircleShape,
        color = CardSurface,
        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingNavItem(
                selected = selectedTab == MainTab.BILLING,
                onClick = { onTabSelected(MainTab.BILLING) },
                icon = Icons.Default.ReceiptLong,
                label = "Cek Tagihan"
            )

            Spacer(modifier = Modifier.width(6.dp))

            FloatingNavItem(
                selected = selectedTab == MainTab.ACCOUNT,
                onClick = { onTabSelected(MainTab.ACCOUNT) },
                icon = Icons.Default.AccountCircle,
                label = "Akun Starlite"
            )
        }
    }
}

@Composable
private fun FloatingNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) PrimaryBlueLight else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "navBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) PrimaryBlueDark else TextSecondary,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "navContent"
    )

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(bgColor)
            .clickable(onClick = onClick)
            .animateContentSize(animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow))
            .padding(horizontal = if (selected) 18.dp else 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )

            AnimatedVisibility(
                visible = selected,
                enter = fadeIn(animationSpec = tween(180)) + expandHorizontally(animationSpec = tween(220)),
                exit = fadeOut(animationSpec = tween(150)) + shrinkHorizontally(animationSpec = tween(200))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = label,
                        color = contentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
