package com.arbdev.sapay.ui.components

import android.bluetooth.BluetoothDevice
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arbdev.sapay.data.model.ReceiptData
import com.arbdev.sapay.data.model.StoreConfig
import com.arbdev.sapay.ui.theme.PrimaryBlue
import com.arbdev.sapay.ui.theme.PrimaryBlueDark
import com.arbdev.sapay.ui.theme.TextPrimary
import com.arbdev.sapay.ui.theme.TextSecondary
import com.arbdev.sapay.ui.util.ReceiptRenderUtil
import com.arbdev.sapay.ui.util.ThermalPrinterUtil
import kotlinx.coroutines.launch
import java.io.File
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ReceiptPreviewModal(
    receiptData: ReceiptData,
    storeConfig: StoreConfig,
    onDismiss: () -> Unit,
    onSaveStoreConfig: (StoreConfig) -> Unit,
    onShowNotification: (String, NotificationType) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showPrinterDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // State biaya admin kustom yang bisa diedit langsung di preview struk
    var currentAdminFee by remember { mutableStateOf(storeConfig.customAdminFee) }
    var isEditingAdminFee by remember { mutableStateOf(false) }
    var adminFeeInput by remember { mutableStateOf(currentAdminFee.toInt().toString()) }

    // Kalkulasi data struk terkini
    val currentReceipt = remember(receiptData, storeConfig, currentAdminFee) {
        val total = receiptData.packageAmount + receiptData.channelFee + currentAdminFee
        receiptData.copy(
            storeName = storeConfig.storeName,
            storeAddress = storeConfig.storeAddress,
            storePhone = storeConfig.storePhone,
            storeLogoPath = storeConfig.storeLogoUri,
            footerMessage = storeConfig.storeFooter,
            customAdminFee = currentAdminFee,
            totalAmount = total
        )
    }

    fun formatRupiah(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFF1F5F9),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Header Bar Modal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = TextPrimary
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pratinjau Nota Pelanggan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Siap dicetak atau dibagikan",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Pengaturan Toko",
                            tint = PrimaryBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Container Struk Kertas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.TopCenter
                ) {
                    // Struk Kertas Thermal Putih Bersih
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 8.dp)
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(16.dp),
                                spotColor = Color.Black.copy(alpha = 0.15f)
                            )
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Logo Toko jika ada
                            val logoBitmap = remember(currentReceipt.storeLogoPath) {
                                currentReceipt.storeLogoPath?.let { path ->
                                    try {
                                        val file = File(path)
                                        if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
                                    } catch (_: Exception) { null }
                                }
                            }

                            if (logoBitmap != null) {
                                Image(
                                    bitmap = logoBitmap.asImageBitmap(),
                                    contentDescription = "Logo Toko",
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // Nama & Alamat Toko
                            Text(
                                text = currentReceipt.storeName.uppercase(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimary,
                                textAlign = TextAlign.Center
                            )

                            if (currentReceipt.storeAddress.isNotBlank()) {
                                Text(
                                    text = currentReceipt.storeAddress,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                            if (currentReceipt.storePhone.isNotBlank() && currentReceipt.storePhone != "-") {
                                Text(
                                    text = "Telp: ${currentReceipt.storePhone}",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            DashedDivider()
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "BUKTI PEMBAYARAN WIFI",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                color = PrimaryBlueDark
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            ReceiptRowText(label = "Waktu", value = currentReceipt.paymentDate)
                            ReceiptRowText(label = "No. Trx", value = currentReceipt.trxCode)
                            ReceiptRowText(label = "Metode", value = currentReceipt.paymentMethod)

                            Spacer(modifier = Modifier.height(10.dp))
                            DashedDivider()
                            Spacer(modifier = Modifier.height(10.dp))

                            ReceiptRowText(label = "ID Pelanggan", value = currentReceipt.customerId, boldValue = true)
                            ReceiptRowText(label = "Nama", value = currentReceipt.customerName, boldValue = true)
                            ReceiptRowText(label = "Paket WiFi", value = currentReceipt.packageName)
                            if (currentReceipt.billingPeriod.isNotBlank()) {
                                ReceiptRowText(label = "Periode", value = currentReceipt.billingPeriod)
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            DashedDivider()
                            Spacer(modifier = Modifier.height(10.dp))

                            // Rincian Biaya
                            ReceiptRowText(label = "Biaya Paket", value = formatRupiah(currentReceipt.packageAmount))
                            if (currentReceipt.channelFee > 0) {
                                ReceiptRowText(label = "Biaya Saluran", value = formatRupiah(currentReceipt.channelFee))
                            }

                            // Biaya Admin Toko (Bisa Diedit Cepat)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Biaya Admin Toko",
                                        fontSize = 12.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = {
                                            isEditingAdminFee = !isEditingAdminFee
                                            adminFeeInput = currentAdminFee.toInt().toString()
                                        },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Ubah Biaya Admin",
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = formatRupiah(currentAdminFee),
                                    fontSize = 12.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }

                            // Form Edit Cepat Biaya Admin
                            AnimatedVisibility(visible = isEditingAdminFee) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = adminFeeInput,
                                        onValueChange = { adminFeeInput = it.filter { c -> c.isDigit() } },
                                        prefix = { Text("Rp ", fontSize = 12.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            val newFee = adminFeeInput.toDoubleOrNull() ?: 0.0
                                            currentAdminFee = newFee
                                            isEditingAdminFee = false
                                            onSaveStoreConfig(storeConfig.copy(customAdminFee = newFee))
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Set", fontSize = 12.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = PrimaryBlueDark, thickness = 2.dp)
                            Spacer(modifier = Modifier.height(10.dp))

                            // TOTAL BAYAR
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TOTAL BAYAR",
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PrimaryBlueDark
                                )
                                Text(
                                    text = formatRupiah(currentReceipt.totalAmount),
                                    fontSize = 16.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PrimaryBlueDark
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            DashedDivider()
                            Spacer(modifier = Modifier.height(14.dp))

                            // Badge Status LUNAS
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFDCFCE7),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF16A34A))
                            ) {
                                Text(
                                    text = "★ L U N A S ★",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF15803D),
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = currentReceipt.footerMessage,
                                fontSize = 11.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "SaPay • Starlite Indonesia Payment",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons Bar di Bawah
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Tombol Cetak Thermal Bluetooth
                    Button(
                        onClick = {
                            showPrinterDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cetak ke Printer Bluetooth",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row Tombol Bagikan WhatsApp (Teks & Gambar)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                ReceiptRenderUtil.shareReceiptAsText(context, currentReceipt)
                                onShowNotification("Membagikan struk teks ke WhatsApp", NotificationType.INFO)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Kirim WA Teks",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val result = ReceiptRenderUtil.shareReceiptAsImage(context, currentReceipt)
                                if (result.isSuccess) {
                                    onShowNotification("Membagikan gambar struk ke WhatsApp", NotificationType.INFO)
                                } else {
                                    onShowNotification(result.exceptionOrNull()?.message ?: "Gagal membagikan gambar", NotificationType.ERROR)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Kirim WA Gambar",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog Pemilihan Printer Bluetooth
    if (showPrinterDialog) {
        PrinterSelectionDialog(
            selectedMac = storeConfig.printerMac,
            onDismiss = { showPrinterDialog = false },
            onPrinterSelected = { device: BluetoothDevice ->
                showPrinterDialog = false
                onShowNotification("Menghubungkan ke ${device.name ?: "Printer"}...", NotificationType.INFO)
                coroutineScope.launch {
                    val printResult = ThermalPrinterUtil.printReceipt(context, device, currentReceipt)
                    if (printResult.isSuccess) {
                        onShowNotification("Nota berhasil dicetak!", NotificationType.SUCCESS)
                        // Simpan printer preference
                        onSaveStoreConfig(
                            storeConfig.copy(
                                printerMac = device.address,
                                printerName = device.name
                            )
                        )
                    } else {
                        onShowNotification(
                            printResult.exceptionOrNull()?.message ?: "Gagal mencetak nota",
                            NotificationType.ERROR
                        )
                    }
                }
            }
        )
    }

    // Dialog Pengaturan Toko & Logo
    if (showSettingsDialog) {
        ReceiptSettingsDialog(
            initialConfig = storeConfig,
            onDismiss = { showSettingsDialog = false },
            onSave = { newConfig ->
                showSettingsDialog = false
                currentAdminFee = newConfig.customAdminFee
                onSaveStoreConfig(newConfig)
                onShowNotification("Pengaturan nota berhasil disimpan", NotificationType.SUCCESS)
            }
        )
    }
}

@Composable
private fun ReceiptRowText(label: String, value: String, boldValue: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = TextSecondary
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (boldValue) FontWeight.Bold else FontWeight.Normal,
            color = TextPrimary
        )
    }
}

@Composable
private fun DashedDivider() {
    Text(
        text = "- - - - - - - - - - - - - - - - - - - - - - - - -",
        color = Color(0xFFCBD5E1),
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}
