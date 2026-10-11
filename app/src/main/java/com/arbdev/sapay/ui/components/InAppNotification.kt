package com.arbdev.sapay.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

enum class NotificationType {
    SUCCESS,
    ERROR,
    INFO,
    WARNING
}

data class InAppNotificationData(
    val id: Long = System.currentTimeMillis(),
    val message: String,
    val type: NotificationType = NotificationType.INFO,
    val durationMs: Long = 3500L
)

@Composable
fun InAppNotificationBanner(
    notification: InAppNotificationData?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(notification?.id) {
        if (notification != null && notification.durationMs > 0) {
            delay(notification.durationMs)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = notification != null,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(dampingRatio = 0.8f)
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = spring(dampingRatio = 0.8f)
        ) + fadeOut(),
        modifier = modifier
    ) {
        if (notification != null) {
            val (bgColor, contentColor, borderColor, icon) = when (notification.type) {
                NotificationType.SUCCESS -> Quad(
                    Color(0xFFF0FDF4),
                    Color(0xFF166534),
                    Color(0xFFBBF7D0),
                    Icons.Default.CheckCircle
                )
                NotificationType.ERROR -> Quad(
                    Color(0xFFFEF2F2),
                    Color(0xFF991B1B),
                    Color(0xFFFECACA),
                    Icons.Default.Error
                )
                NotificationType.WARNING -> Quad(
                    Color(0xFFFFFBEB),
                    Color(0xFF92400E),
                    Color(0xFFFDE68A),
                    Icons.Default.Warning
                )
                NotificationType.INFO -> Quad(
                    Color(0xFFEFF6FF),
                    Color(0xFF1E40AF),
                    Color(0xFFBFDBFE),
                    Icons.Default.Info
                )
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .shadow(
                        elevation = 10.dp,
                        shape = RoundedCornerShape(16.dp),
                        spotColor = contentColor.copy(alpha = 0.25f)
                    )
                    .border(1.dp, borderColor, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                color = bgColor
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(contentColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = notification.message,
                        color = contentColor,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 18.sp,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = contentColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
