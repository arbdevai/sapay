package com.arbdev.sapay.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arbdev.sapay.data.model.PaymentChannelGroup
import com.arbdev.sapay.data.model.PaymentChannelItem
import com.arbdev.sapay.ui.theme.*
import com.arbdev.sapay.ui.viewmodel.BillingUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentChannelScreen(
    state: BillingUiState,
    onBackClick: () -> Unit,
    onSelectChannel: (PaymentChannelItem) -> Unit,
    onConfirmPayment: () -> Unit,
    onRetry: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Metode Pembayaran",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryBlue
                )
            )
        },
        bottomBar = {
            Surface(
                color = CardSurface,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = onConfirmPayment,
                        enabled = state.selectedChannel != null && !state.isLoading,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text = if (state.selectedChannel != null) {
                                    "Bayar dengan ${state.selectedChannel.name}"
                                } else {
                                    "Pilih Metode Pembayaran"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundLight)
                .padding(paddingValues)
        ) {
            if (state.isLoading && state.paymentChannels.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            } else if (state.paymentChannels.isEmpty() && !state.errorMessage.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = ErrorRed,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Gagal Memuat Metode Pembayaran",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = state.errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onBackClick,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Kembali")
                                }
                                Button(
                                    onClick = onRetry,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Coba Lagi")
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    item {
                        Text(
                            text = "Pilih Saluran Pembayaran",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Pilih salah satu metode pembayaran yang kamu inginkan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    items(state.paymentChannels) { group ->
                        if (group.paymentChannels.isNotEmpty()) {
                            PaymentGroupSection(
                                group = group,
                                selectedChannelId = state.selectedChannel?.id,
                                onSelectChannel = onSelectChannel
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentGroupSection(
    group: PaymentChannelGroup,
    selectedChannelId: String?,
    onSelectChannel: (PaymentChannelItem) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val icon = when {
                    group.name?.contains("QRIS", ignoreCase = true) == true -> Icons.Default.QrCode
                    group.name?.contains("Virtual", ignoreCase = true) == true -> Icons.Default.AccountBalance
                    group.name?.contains("Outlet", ignoreCase = true) == true -> Icons.Default.Store
                    else -> Icons.Default.CreditCard
                }

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = group.name ?: "Metode Lain",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlueDark
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            group.paymentChannels.forEach { channel ->
                val isSelected = channel.id == selectedChannelId

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) PrimaryBlueLight else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) PrimaryBlue else BorderColor,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectChannel(channel) }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelectChannel(channel) },
                        colors = RadioButtonDefaults.colors(selectedColor = PrimaryBlue)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = channel.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )

                        val feeText = when {
                            channel.feeFlat != null && channel.feeFlat != "0" -> {
                                "Biaya Admin: Rp ${channel.feeFlat}"
                            }
                            channel.feePercent != null && channel.feePercent != "0.000" -> {
                                "Biaya: ${channel.feePercent}%"
                            }
                            else -> "Bebas Biaya Admin"
                        }

                        Text(
                            text = feeText,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (feeText.contains("Bebas")) SuccessGreen else TextSecondary
                        )
                    }
                }
            }
        }
    }
}
