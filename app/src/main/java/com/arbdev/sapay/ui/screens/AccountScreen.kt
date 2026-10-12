package com.arbdev.sapay.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arbdev.sapay.BuildConfig
import com.arbdev.sapay.data.model.BillHistoryItem
import com.arbdev.sapay.data.model.ReceiptData
import com.arbdev.sapay.data.model.StoreConfig
import com.arbdev.sapay.data.model.UserProfile
import com.arbdev.sapay.ui.components.NotificationType
import com.arbdev.sapay.ui.components.ReceiptPreviewModal
import com.arbdev.sapay.ui.theme.*
import com.arbdev.sapay.ui.viewmodel.AuthUiState
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    state: AuthUiState,
    onPhoneChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onRequestLogout: () -> Unit,
    onDismissLogout: () -> Unit,
    onRequestForgotPassword: () -> Unit,
    onDismissForgotPassword: () -> Unit,
    onSendResetPassword: (String) -> Unit,
    onRefresh: () -> Unit,
    onPayFromHistory: (BillHistoryItem) -> Unit,
    onRenewPackageClick: (UserProfile) -> Unit,
    onCheckUpdateClick: () -> Unit,
    storeConfig: StoreConfig = StoreConfig(),
    onSaveStoreConfig: (StoreConfig) -> Unit = {},
    onShowNotification: (String, NotificationType) -> Unit = { _, _ -> }
) {
    var passwordVisible by remember { mutableStateOf(false) }
    var resetPhoneInput by remember(state.phoneInput) { mutableStateOf(state.phoneInput) }
    var selectedReceiptItem by remember { mutableStateOf<BillHistoryItem?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (state.isLoggedIn) PrimaryBlueLight else PrimaryBlue.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (state.isLoggedIn) Icons.Default.Person else Icons.Default.Lock,
                            contentDescription = null,
                            tint = PrimaryBlue
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (state.isLoggedIn) "Akun Starlite" else "Login Akun",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (state.isLoggedIn) {
                                state.profile?.name ?: "Detail layanan & invoice"
                            } else {
                                "Masuk untuk cek paket & riwayat tagihan"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                }

                if (state.isLoggedIn) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onRefresh) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Muat Ulang",
                                tint = PrimaryBlue
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        OutlinedButton(
                            onClick = onRequestLogout,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                            border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Keluar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        if (!state.isLoggedIn) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(22.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(PrimaryBlueLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Wifi,
                                    contentDescription = null,
                                    tint = PrimaryBlue
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Login Starlite",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlueDark
                                )
                                Text(
                                    text = "Gunakan nomor HP & password akun Anda",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = state.phoneInput,
                            onValueChange = { input ->
                                onPhoneChanged(input.filter { it.isDigit() })
                            },
                            label = { Text("Nomor Handphone") },
                            placeholder = { Text("08123456789") },
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = PrimaryBlue)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = state.passwordInput,
                            onValueChange = onPasswordChanged,
                            label = { Text("Password") },
                            placeholder = { Text("Masukkan password akun") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryBlue)
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = onRequestForgotPassword,
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Lupa Password?",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue
                                )
                            }
                        }

                        if (!state.errorMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = ErrorRedLight),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = state.errorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ErrorRed,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onLoginClick,
                            enabled = !state.isLoading,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Text("Masuk ke Akun", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = PrimaryBlueLight.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Belum punya akun Starlite? Silakan mendaftar sebagai pelanggan di starliteindonesia.com",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PrimaryBlueDark,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Logged in: Profile Card (Persis seperti web Starlite)
            val profile = state.profile
            if (profile != null) {
                item {
                    UserProfileDetailsCard(
                        profile = profile,
                        onRenewPackage = { onRenewPackageClick(profile) },
                        onLogout = onRequestLogout
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
            } else if (state.isLoadingProfile) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PrimaryBlue)
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            } else {
                // Profile null and not loading (e.g. error loading profile)
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = ErrorRed,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Gagal Memuat Profil Akun",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = state.errorMessage ?: "Terjadi kesalahan saat memuat data profil akun Anda.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onRequestLogout,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                                    border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Logout,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Keluar Akun")
                                }
                                Button(
                                    onClick = onRefresh,
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Coba Lagi")
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // History Header
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Riwayat Pembayaran & Tagihan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    if (state.isLoadingHistory) {
                        CircularProgressIndicator(
                            color = PrimaryBlue,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (state.history.isEmpty() && !state.isLoadingHistory) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tidak ada data riwayat transaksi.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(state.history) { item ->
                    WebHistoryItemCard(
                        item = item,
                        onPayClick = { onPayFromHistory(item) },
                        onPrintReceiptClick = { selectedReceiptItem = item }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))

            // App Version & In-App Updater Section
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Tentang SaPay",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Aplikasi Resmi Pelanggan WiFi Starlite • v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = onCheckUpdateClick,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Periksa Pembaruan Aplikasi")
                    }
                }
            }
        }
    }

    if (state.showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = onDismissLogout,
            icon = {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Keluar dari Akun?",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Apakah Anda yakin ingin keluar dari akun Starlite? Anda harus memasukkan nomor HP dan password kembali untuk melihat invoice dan profil Anda.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = onLogoutClick,
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Ya, Keluar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissLogout,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Batal")
                }
            }
        )
    }

    if (state.showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = onDismissForgotPassword,
            icon = {
                Icon(
                    imageVector = Icons.Default.LockReset,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Lupa Password Akun",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Masukkan nomor handphone akun Starlite Anda. Sistem akan mengirim link reset password resmi via WhatsApp:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = resetPhoneInput,
                        onValueChange = { resetPhoneInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Nomor Handphone") },
                        placeholder = { Text("08123456789") },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = PrimaryBlue)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { onSendResetPassword(resetPhoneInput) },
                    enabled = !state.isResettingPassword && resetPhoneInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (state.isResettingPassword) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Kirim Link WA", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissForgotPassword,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Batal")
                }
            }
        )
    }

    if (selectedReceiptItem != null) {
        val item = selectedReceiptItem!!
        val packageName = item.regionalPackage?.packageInfo?.name ?: item.regionalPackage?.name ?: "Paket Internet WiFi"
        val rawSettlementDate = item.transactionList.firstOrNull()?.settlementDate
        val settlementDate = formatIndonesianDate(rawSettlementDate, withTime = true)
        val dueDate = formatIndonesianDate(item.dueDate, withTime = false)
        val customerId = state.profile?.customerId ?: state.phoneInput
        val customerName = state.profile?.name ?: "Pelanggan Starlite"
        val amount = item.amount ?: item.regionalPackage?.price ?: 0.0

        ReceiptPreviewModal(
            receiptData = ReceiptData(
                storeName = storeConfig.storeName,
                storeAddress = storeConfig.storeAddress,
                storePhone = storeConfig.storePhone,
                storeLogoPath = storeConfig.storeLogoUri,
                trxCode = item.code ?: "INV-${item.id?.take(8) ?: System.currentTimeMillis()}",
                paymentDate = if (settlementDate != "-") settlementDate else dueDate,
                customerId = customerId,
                customerName = customerName,
                packageName = packageName,
                billingPeriod = dueDate,
                packageAmount = amount,
                channelFee = 0.0,
                customAdminFee = storeConfig.customAdminFee,
                totalAmount = amount + storeConfig.customAdminFee,
                paymentMethod = "Lunas",
                status = "LUNAS",
                footerMessage = storeConfig.storeFooter
            ),
            storeConfig = storeConfig,
            onDismiss = { selectedReceiptItem = null },
            onSaveStoreConfig = onSaveStoreConfig,
            onShowNotification = onShowNotification
        )
    }
}

@Composable
fun UserProfileDetailsCard(
    profile: UserProfile,
    onRenewPackage: () -> Unit,
    onLogout: () -> Unit
) {
    val activePkg = profile.activeRegionalPackage

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Customer Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.name ?: "Pelanggan Starlite",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlueDark
                    )
                    Text(
                        text = "ID: ${profile.customerId ?: "-"}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                }

                val status = profile.status ?: "active"
                val (badgeColor, badgeBg, statusLabel) = when (status.lowercase()) {
                    "active" -> Triple(SuccessGreen, SuccessGreenLight, "Aktif")
                    "nearly-expired" -> Triple(WarningYellow, AccentOrangeLight, "Segera Berakhir")
                    "dismantle", "isolated" -> Triple(ErrorRed, ErrorRedLight, "Nonaktif")
                    else -> Triple(PrimaryBlue, PrimaryBlueLight, status)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Contact info
            DetailItem(label = "Nomor Telepon", value = profile.phoneNumber ?: "-")
            if (!profile.email.isNullOrBlank()) {
                DetailItem(label = "Email", value = profile.email)
            }
            if (!profile.address.isNullOrBlank()) {
                DetailItem(label = "Alamat", value = profile.address)
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = BorderColor)
            Spacer(modifier = Modifier.height(16.dp))

            // Active Package Card
            Text(
                text = "Paket Internet Aktif",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlueDark
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = activePkg?.name ?: activePkg?.packageInfo?.name ?: "Paket Reguler",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (!activePkg?.speedUpTo.isNullOrBlank()) {
                        Text(
                            text = "Kecepatan: ${activePkg?.speedUpTo}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                if (activePkg?.price != null && activePkg.price > 0) {
                    val formatted = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(activePkg.price)
                    Text(
                        text = formatted,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Expiry Date & Remaining Days (Persis seperti di web Starlite)
            val expireDateStr = profile.activePackageExpireDate
            var daysRemaining = "-"
            val formattedDueDate = formatIndonesianDate(expireDateStr, withTime = false)

            if (!expireDateStr.isNullOrBlank()) {
                try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val expireDate = sdf.parse(expireDateStr.substring(0, 10))
                    if (expireDate != null) {
                        val diff = expireDate.time - System.currentTimeMillis()
                        val days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) + 1
                        daysRemaining = if (days >= 0) "$days Hari" else "Lewat Jatuh Tempo"
                    }
                } catch (_: Exception) {}
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PrimaryBlueLight)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Jatuh Tempo", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text(text = formattedDueDate, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = PrimaryBlueDark)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Sisa Hari", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text(text = daysRemaining, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (daysRemaining.contains("Lewat")) ErrorRed else PrimaryBlueDark)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action Button "Perpanjang / Bayar Paket"
            Button(
                onClick = onRenewPackage,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Payment,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Perpanjang Paket / Bayar Tagihan", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onLogout,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Keluar dari Akun Starlite", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun WebHistoryItemCard(
    item: BillHistoryItem,
    onPayClick: () -> Unit,
    onPrintReceiptClick: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val isPaid = item.isPaid == true

            // Date & Status calculation
            val formattedDueDate = formatIndonesianDate(item.dueDate, withTime = false)
            var isLate = false

            if (!item.dueDate.isNullOrBlank()) {
                try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val date = sdf.parse(item.dueDate.substring(0, 10))
                    if (date != null) {
                        isLate = Date().after(date)
                    }
                } catch (_: Exception) {}
            }

            // Settlement date if paid (formatted with Indonesian full month name e.g. 11 Oktober 2026, 14:30 WIB)
            val settlementRaw = item.transactionList.firstOrNull { it.paymentStatus == "settle" }?.settlementDate
            val formattedSettlement = formatIndonesianDate(settlementRaw, withTime = true)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                val packageName = item.regionalPackage?.packageInfo?.name
                    ?: item.regionalPackage?.name
                    ?: "Langganan WiFi"

                Text(
                    text = packageName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlueDark
                )

                val (badgeBg, badgeColor, statusText) = when {
                    isPaid -> Triple(SuccessGreenLight, SuccessGreen, "Tagihan Lunas")
                    isLate -> Triple(ErrorRedLight, ErrorRed, "Lewat Jatuh Tempo")
                    else -> Triple(AccentOrangeLight, AccentOrange, "Belum Dibayar")
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val amount = item.amount ?: item.regionalPackage?.price ?: 0.0
            val formatted = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount)
            Text(
                text = "$formatted / bulan",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (isPaid && !settlementRaw.isNullOrBlank()) {
                Text(
                    text = "Lunas dibayarkan pada $formattedSettlement",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = SuccessGreen
                )
            } else {
                Text(
                    text = "Jatuh tempo: $formattedDueDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isLate) ErrorRed else TextSecondary
                )
            }

            if (!item.code.isNullOrBlank()) {
                Text(
                    text = "Kode Invoice: ${item.code}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            if (!isPaid) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onPayClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                ) {
                    Text("Bayar Sekarang", fontWeight = FontWeight.Bold)
                }
            } else if (onPrintReceiptClick != null) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onPrintReceiptClick,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cetak Nota / Struk Kasir",
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryBlue
                    )
                }
            }
        }
    }
}

/**
 * Format string tanggal (ISO-8601, Timestamp, atau YYYY-MM-DD)
 * menjadi format bahasa Indonesia resmi dengan nama bulan lengkap
 * Contoh: "11 Oktober 2026, 15:45 WIB" atau "11 Oktober 2026"
 */
fun formatIndonesianDate(rawDateStr: String?, withTime: Boolean = true): String {
    if (rawDateStr.isNullOrBlank()) return "-"
    val formats = listOf(
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") },
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") },
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US),
        SimpleDateFormat("yyyy-MM-dd", Locale.US)
    )

    for (sdf in formats) {
        try {
            val date = sdf.parse(rawDateStr)
            if (date != null) {
                val hasTime = rawDateStr.contains("T") || rawDateStr.contains(":")
                val pattern = if (withTime && hasTime) {
                    "dd MMMM yyyy, HH:mm 'WIB'"
                } else {
                    "dd MMMM yyyy"
                }
                val outputFormat = SimpleDateFormat(pattern, Locale("id", "ID")).apply {
                    if (hasTime) {
                        timeZone = java.util.TimeZone.getTimeZone("Asia/Jakarta")
                    }
                }
                return outputFormat.format(date)
            }
        } catch (_: Exception) {}
    }

    // Fallback: jika dimulai dengan YYYY-MM-DD
    return try {
        if (rawDateStr.length >= 10 && rawDateStr[4] == '-' && rawDateStr[7] == '-') {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(rawDateStr.substring(0, 10))
            if (date != null) {
                SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(date)
            } else rawDateStr
        } else rawDateStr
    } catch (_: Exception) {
        rawDateStr
    }
}
