package com.arbdev.sapay.ui.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.arbdev.sapay.MainActivity
import com.arbdev.sapay.R
import java.text.NumberFormat
import java.util.Locale

object StatusBarNotificationUtil {

    const val CHANNEL_ID_BILLING = "starlite_billing_reminder"
    const val CHANNEL_ID_PAYMENT = "starlite_payment_status"
    private const val NOTIFICATION_ID_BILL = 9001
    private const val NOTIFICATION_ID_PENDING_PAYMENT = 9002

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val billChannel = NotificationChannel(
                CHANNEL_ID_BILLING,
                "Pengingat Tagihan WiFi",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi pengingat jatuh tempo tagihan internet Starlite"
                enableVibration(true)
                setShowBadge(true)
            }

            val payChannel = NotificationChannel(
                CHANNEL_ID_PAYMENT,
                "Status & Transaksi Pembayaran",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifikasi status transaksi pembayaran tagihan WiFi"
            }

            manager.createNotificationChannel(billChannel)
            manager.createNotificationChannel(payChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun showBillDueNotification(
        context: Context,
        customerName: String,
        customerId: String,
        amount: Double,
        dueDate: String,
        packageName: String
    ) {
        if (!hasNotificationPermission(context)) return
        createNotificationChannels(context)

        val formattedAmount = try {
            NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount)
        } catch (_: Exception) {
            "Rp ${amount.toLong()}"
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("extra_customer_id", customerId)
            putExtra("extra_action", "view_bill")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_BILL,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "Tagihan WiFi Starlite Belum Dibayar"
        val shortContent = "Halo $customerName, tagihan $packageName ($formattedAmount) jatuh tempo $dueDate. Segera bayar!"
        val bigText = "Halo $customerName ($customerId),\n\nTagihan internet WiFi $packageName sebesar $formattedAmount jatuh tempo pada $dueDate.\n\nSegera selesaikan pembayaran agar jaringan internet tidak terputus otomatis!"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_BILLING)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(shortContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setColor(0xFF084C94.toInt())
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_send, "Bayar Sekarang", pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_BILL, notification)
        } catch (_: SecurityException) {}
    }

    fun showPendingPaymentNotification(
        context: Context,
        customerName: String,
        trxCode: String,
        amount: Double,
        paymentMethod: String
    ) {
        if (!hasNotificationPermission(context)) return
        createNotificationChannels(context)

        val formattedAmount = try {
            NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount)
        } catch (_: Exception) {
            "Rp ${amount.toLong()}"
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("extra_trx_code", trxCode)
            putExtra("extra_action", "view_payment")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_PENDING_PAYMENT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "Menunggu Pembayaran $paymentMethod"
        val shortContent = "Transaksi $trxCode sebesar $formattedAmount belum dibayar. Selesaikan sekarang."
        val bigText = "Halo $customerName,\n\nTransaksi pembayaran WiFi via $paymentMethod ($trxCode) sebesar $formattedAmount sedang menunggu pembayaran Anda. Buka aplikasi untuk menyelesaikan pembayaran."

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_PAYMENT)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(shortContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setColor(0xFF084C94.toInt())
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_PENDING_PAYMENT, notification)
        } catch (_: SecurityException) {}
    }

    fun clearBillNotification(context: Context) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.cancel(NOTIFICATION_ID_BILL)
            manager?.cancel(NOTIFICATION_ID_PENDING_PAYMENT)
        } catch (_: Exception) {}
    }
}
