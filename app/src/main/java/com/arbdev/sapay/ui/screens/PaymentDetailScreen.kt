package com.arbdev.sapay.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arbdev.sapay.data.model.ReceiptData
import com.arbdev.sapay.data.model.StoreConfig
import com.arbdev.sapay.ui.components.NotificationType
import com.arbdev.sapay.ui.components.ReceiptPreviewModal
import com.arbdev.sapay.ui.theme.*
import com.arbdev.sapay.ui.util.QrisDownloadUtil
import com.arbdev.sapay.ui.util.QrCodeGenerator
import com.arbdev.sapay.ui.viewmodel.BillingUiState
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentDetailScreen(
    state: BillingUiState,
    onBackClick: () -> Unit,
    onCheckStatus: (String) -> Unit,
    onSaveStoreConfig: (StoreConfig) -> Unit = {},
    onShowNotification: (String, NotificationType) -> Unit = { _, _ -> }
) {
    BackHandler(onBack = onBackClick)
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val payment = state.currentPayment
    var showReceiptModal by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Instruksi Pembayaran",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryBlue
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundLight)
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(20.dp)
        ) {
            if (payment == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Memuat data pembayaran...")
                }
                return@Scaffold
            }

            // Total Amount Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryBlueDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Total Pembayaran",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val amount = payment.grandTotal ?: 0.0
                    val formattedAmount = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount)
                    Text(
                        text = formattedAmount,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = AccentOrange
                    )

                    if (!payment.trxCode.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Kode Transaksi: ${payment.trxCode}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Success Card if paid
            if (state.isPaymentSuccess || payment.paymentStatus == "settle") {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SuccessGreenLight),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SuccessGreen.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Pembayaran Berhasil!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tagihan WiFi Anda telah lunas dan layanan internet aktif normal.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = { showReceiptModal = true },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Cetak Nota / Struk Kasir",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
                // Jika pembayaran sudah sukses, jangan render petunjuk transfer lagi
                return@Column
            }

            // Method Specific Display
            val isQris = payment.paymentMethod?.contains("QRIS", ignoreCase = true) == true
            val isVa = payment.paymentMethod?.contains("VIRTUAL_ACCOUNT", ignoreCase = true) == true || payment.va?.va != null
            val isOutlet = payment.paymentMethod?.contains("INDOMARET", ignoreCase = true) == true ||
                    payment.paymentMethod?.contains("ALFAMART", ignoreCase = true) == true

            if (isQris) {
                val qrisPayload = payment.paymentNumber
                    ?: payment.paymentPayload?.actions?.firstOrNull()?.value
                    ?: ""

                QrisPaymentCard(
                    qrisPayload = qrisPayload,
                    customerName = state.customerInfo?.name ?: "",
                    customerId = state.customerInfo?.customerId ?: state.queryInput,
                    amount = payment.grandTotal ?: 0.0,
                    trxCode = payment.trxCode ?: "",
                    onShowNotification = onShowNotification
                )
            } else if (isVa) {
                val vaNumber = payment.va?.va ?: payment.paymentNumber ?: ""
                VaPaymentCard(
                    bankName = payment.paymentMethod ?: "Virtual Account",
                    vaNumber = vaNumber,
                    context = context
                )
            } else if (isOutlet) {
                val code = payment.paymentCode ?: payment.paymentNumber ?: ""
                OutletPaymentCard(
                    storeName = payment.paymentMethod ?: "Minimarket",
                    paymentCode = code,
                    context = context
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Check Status Button
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Status Pembayaran",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Setelah mentransfer, klik tombol di bawah untuk verifikasi status secara otomatis.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    if (!state.statusMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = state.statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryBlue
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val code = payment.trxCode ?: ""
                            if (code.isNotBlank()) {
                                onCheckStatus(code)
                            }
                        },
                        enabled = !state.isCheckingStatus,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        if (state.isCheckingStatus) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Cek Status Pembayaran",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tombol Pratinjau Nota untuk Kasir
                    OutlinedButton(
                        onClick = { showReceiptModal = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pratinjau & Cetak Nota Kasir",
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryBlue
                        )
                    }
                }
            }
        }
    }

    if (showReceiptModal && payment != null) {
        val todayStr = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID")).format(Date())
        val pkg = state.selectedPackage ?: state.customerInfo?.activeRegionalPackage ?: state.customerInfo?.regionalPackage
        val packageName = pkg?.packageInfo?.name ?: pkg?.name ?: "Paket Internet Starlite"
        val packagePrice = pkg?.price ?: payment.grandTotal ?: 0.0
        val channelFee = ((payment.grandTotal ?: 0.0) - packagePrice).coerceAtLeast(0.0)
        val billingPeriod = state.customerInfo?.activePackageExpireDate ?: ""
        val customAdminFee = state.storeConfig.customAdminFee
        val totalAmount = (payment.grandTotal ?: packagePrice) + customAdminFee

        ReceiptPreviewModal(
            receiptData = ReceiptData(
                storeName = state.storeConfig.storeName,
                storeAddress = state.storeConfig.storeAddress,
                storePhone = state.storeConfig.storePhone,
                storeLogoPath = state.storeConfig.storeLogoUri,
                trxCode = payment.trxCode ?: "-",
                paymentDate = todayStr,
                customerId = state.customerInfo?.customerId ?: state.queryInput,
                customerName = state.customerInfo?.name ?: "Pelanggan Starlite",
                packageName = packageName,
                billingPeriod = billingPeriod,
                packageAmount = packagePrice,
                channelFee = channelFee,
                customAdminFee = customAdminFee,
                totalAmount = totalAmount,
                paymentMethod = payment.paymentMethod ?: "Online",
                status = if (state.isPaymentSuccess || payment.paymentStatus == "settle") "LUNAS" else "PENDING",
                footerMessage = state.storeConfig.storeFooter
            ),
            storeConfig = state.storeConfig,
            onDismiss = { showReceiptModal = false },
            onSaveStoreConfig = onSaveStoreConfig,
            onShowNotification = onShowNotification
        )
    }
}

@Composable
fun QrisPaymentCard(
    qrisPayload: String,
    customerName: String = "",
    customerId: String = "",
    amount: Double = 0.0,
    trxCode: String = "",
    onShowNotification: (String, NotificationType) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Scan QRIS Starlite",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlueDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Bisa di-scan dari semua m-Banking & E-Wallet",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            val bitmap = remember(qrisPayload) {
                if (qrisPayload.isNotBlank()) {
                    QrCodeGenerator.generateQrBitmap(qrisPayload, 600)
                } else null
            }

            if (bitmap != null) {
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "QRIS Code",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tombol Unduh & Bagikan QRIS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val poster = QrisDownloadUtil.createQrisPosterBitmap(
                                qrBitmap = bitmap,
                                customerName = customerName,
                                customerId = customerId,
                                amount = amount,
                                trxCode = trxCode
                            )
                            val saveResult = QrisDownloadUtil.saveQrisToGallery(context, poster, trxCode)
                            if (saveResult.isSuccess) {
                                onShowNotification("Gambar QRIS berhasil disimpan ke Galeri!", NotificationType.SUCCESS)
                            } else {
                                onShowNotification(
                                    saveResult.exceptionOrNull()?.message ?: "Gagal menyimpan QRIS",
                                    NotificationType.ERROR
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Unduh QRIS",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryBlue
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val poster = QrisDownloadUtil.createQrisPosterBitmap(
                                qrBitmap = bitmap,
                                customerName = customerName,
                                customerId = customerId,
                                amount = amount,
                                trxCode = trxCode
                            )
                            val caption = "Kode QRIS Pembayaran WiFi Starlite\nAtas Nama: $customerName ($customerId)\nTotal Tagihan: ${NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount)}"
                            val shareResult = QrisDownloadUtil.shareQrisImage(context, poster, caption)
                            if (!shareResult.isSuccess) {
                                onShowNotification(
                                    shareResult.exceptionOrNull()?.message ?: "Gagal membagikan QRIS",
                                    NotificationType.ERROR
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Kirim ke WA",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(BorderColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Memuat QRIS...", color = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Dukungan: BCA, Mandiri Livin, BRImo, BNI, GoPay, OVO, Dana, ShopeePay, LinkAja",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun VaPaymentCard(bankName: String, vaNumber: String, context: Context) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Nomor Virtual Account",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = bankName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlueDark
            )

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(PrimaryBlueLight)
                    .border(1.dp, PrimaryBlue.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (vaNumber.isNotBlank()) vaNumber else "-",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryBlueDark,
                        letterSpacing = 1.sp
                    )

                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("VA Number", vaNumber))
                            Toast.makeText(context, "Nomor VA berhasil disalin!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Salin Nomor VA",
                            tint = PrimaryBlue
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Transfer tepat sesuai nominal tagihan melalui ATM, Mobile Banking, atau Internet Banking ke nomor Virtual Account di atas.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun OutletPaymentCard(storeName: String, paymentCode: String, context: Context) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Kode Pembayaran Gerai",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = storeName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlueDark
            )

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(PrimaryBlueLight)
                    .border(1.dp, PrimaryBlue.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (paymentCode.isNotBlank()) paymentCode else "-",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryBlueDark
                    )

                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Kode Pembayaran", paymentCode))
                            Toast.makeText(context, "Kode pembayaran berhasil disalin!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Salin",
                            tint = PrimaryBlue
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Tunjukkan kode ini kepada kasir minimarket dan bayar sesuai total tagihan.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}
