package com.arbdev.sapay.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.arbdev.sapay.data.model.CustomerInfo
import com.arbdev.sapay.data.model.RegionalPackage
import com.arbdev.sapay.ui.components.BannerCarousel
import com.arbdev.sapay.ui.theme.*
import com.arbdev.sapay.ui.viewmodel.BillingUiState
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickBillScreen(
    state: BillingUiState,
    updateAvailable: Boolean = false,
    latestReleaseTag: String? = null,
    onUpdateClick: () -> Unit = {},
    onQueryChanged: (String) -> Unit,
    onCheckBill: () -> Unit,
    onConfirmCheckboxChanged: (Boolean) -> Unit,
    onSelectPackage: (RegionalPackage) -> Unit,
    onProceedToPayment: () -> Unit,
    onViewCurrentPayment: () -> Unit,
    onReactivateClick: () -> Unit,
    onDismissReactivation: () -> Unit,
    onNavigateToAccount: () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Hero Header Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = PrimaryBlue),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "SaPay",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Pembayaran WiFi Starlite",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Cek & bayar tagihan internet tanpa login web, tepat waktu dan sat-set.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    lineHeight = 18.sp
                )
            }
        }

        // In-App Update Notice Banner (if new version detected)
        if (updateAvailable) {
            Spacer(modifier = Modifier.height(14.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onUpdateClick)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFDE68A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = Color(0xFFB45309),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pembaruan Versi ${latestReleaseTag ?: "Baru"} Tersedia",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                        Text(
                            text = "Sentuh untuk mengunduh & memasang fitur terbaru.",
                            fontSize = 11.5.sp,
                            color = Color(0xFFB45309)
                        )
                    }
                    Button(
                        onClick = onUpdateClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Update", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // Web Starlite Image Slider / Promo Carousel
        if (state.banners.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            BannerCarousel(banners = state.banners)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Account Navigation Banner
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                .clickable(onClick = onNavigateToAccount)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlueLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Masuk ke Akun Starlite",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlueDark
                    )
                    Text(
                        text = "Kelola paket, lihat profil, & riwayat invoice lengkap",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search Input Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Nomor Pelanggan / HP",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Masukkan Nomor Pelanggan atau Nomor Telepon Pelanggan",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = state.queryInput,
                    onValueChange = onQueryChanged,
                    placeholder = { Text("Nomor Pelanggan / Nomor Telepon") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = PrimaryBlue
                        )
                    },
                    trailingIcon = {
                        if (state.queryInput.isNotBlank()) {
                            IconButton(onClick = { onQueryChanged("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Hapus",
                                    tint = TextSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            focusManager.clearFocus()
                            onCheckBill()
                        }
                    ),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = BorderColor,
                        focusedContainerColor = CardSurface,
                        unfocusedContainerColor = CardSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        onCheckBill()
                    },
                    enabled = !state.isLoading && state.queryInput.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cek Tagihan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        // Error message if any (and not reactivation)
        if (!state.errorMessage.isNullOrBlank() && !state.needsReactivation) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ErrorRedLight),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ErrorRed.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = ErrorRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = state.errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ErrorRed
                    )
                }
            }
        }

        // Customer Result Card (Konfirmasi Data Pelanggan persis seperti web)
        if (state.customerInfo != null) {
            Spacer(modifier = Modifier.height(20.dp))
            ConfirmationDataCard(
                customer = state.customerInfo,
                selectedPackage = state.selectedPackage,
                availablePackages = state.availablePackages,
                isDataConfirmed = state.isDataConfirmed,
                isLoading = state.isLoading,
                onConfirmCheckboxChanged = onConfirmCheckboxChanged,
                onSelectPackage = onSelectPackage,
                onProceedToPayment = onProceedToPayment,
                hasActivePayment = state.currentPayment != null && !state.currentPayment.trxCode.isNullOrBlank(),
                onViewCurrentPayment = onViewCurrentPayment
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Info Tips Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PrimaryBlueLight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Informasi Pembayaran",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlueDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Pelanggan dapat melakukan pengecekan dan pembayaran tagihan sebelum atau pada tanggal jatuh tempo. Jika layanan dalam masa dismantle, Anda dapat mereaktivasi akun secara otomatis.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary.copy(alpha = 0.85f),
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }

    // Modal Reaktivasi (Sesuai Web Starlite)
    if (state.needsReactivation) {
        Dialog(onDismissRequest = onDismissReactivation) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = CardSurface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Layanan Kamu Dalam Masa",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlueDark
                    )
                    Text(
                        text = "Dismantle (Nonaktif Sementara)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ErrorRed
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Layanan internet kamu saat ini sudah memasuki masa dismantle (Nonaktif Sementara). Untuk mengaktifkan kembali layanan dan melanjutkan pembayaran tagihan, silakan klik tombol “Reaktivasi” di bawah ini.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = onDismissReactivation,
                            shape = RoundedCornerShape(12.dp),
                            enabled = !state.isReactivating
                        ) {
                            Text("Batal", color = ErrorRed)
                        }

                        Button(
                            onClick = onReactivateClick,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                            enabled = !state.isReactivating
                        ) {
                            if (state.isReactivating) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Reaktivasi", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConfirmationDataCard(
    customer: CustomerInfo,
    selectedPackage: RegionalPackage?,
    availablePackages: List<RegionalPackage>,
    isDataConfirmed: Boolean,
    isLoading: Boolean,
    onConfirmCheckboxChanged: (Boolean) -> Unit,
    onSelectPackage: (RegionalPackage) -> Unit,
    onProceedToPayment: () -> Unit,
    hasActivePayment: Boolean,
    onViewCurrentPayment: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Konfirmasi Data Pelanggan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlueDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Pastikan data berikut sudah sesuai sebelum melanjutkan pembayaran:",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            DetailItem(label = "ID Pelanggan", value = customer.customerId ?: "-")
            DetailItem(label = "Nama Lengkap", value = customer.name ?: "-")
            DetailItem(label = "Nomor Telepon", value = customer.phoneNumber ?: "-")

            val activePkg = selectedPackage ?: customer.regionalPackage ?: customer.activeRegionalPackage
            if (activePkg != null) {
                DetailItem(label = "Paket Internet", value = activePkg.name ?: "-")

                if (!activePkg.speedUpTo.isNullOrBlank()) {
                    DetailItem(label = "Kecepatan", value = activePkg.speedUpTo)
                }

                if (activePkg.price != null && activePkg.price > 0) {
                    val formatted = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(activePkg.price)
                    DetailItem(label = "Total Tagihan", value = formatted, isHighlight = true)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Checkbox Persetujuan (Persis seperti di web Starlite)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onConfirmCheckboxChanged(!isDataConfirmed) }
                    .padding(vertical = 4.dp)
            ) {
                Checkbox(
                    checked = isDataConfirmed,
                    onCheckedChange = { onConfirmCheckboxChanged(it) },
                    colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Anda yakin bahwa data pelanggan Anda sudah benar?",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onProceedToPayment,
                enabled = isDataConfirmed && !isLoading,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDataConfirmed) AccentOrange else Color.Gray
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Lanjutkan ke Pembayaran",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            if (hasActivePayment) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onViewCurrentPayment,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Lihat Kode Bayar / QRIS Berjalan")
                }
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String, isHighlight: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isHighlight) PrimaryBlue else TextPrimary
        )
    }
}

