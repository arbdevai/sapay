package com.arbdev.sapay.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.arbdev.sapay.data.api.ApiClient
import com.arbdev.sapay.data.preferences.AppPreferences
import com.arbdev.sapay.data.repository.AuthRepository
import com.arbdev.sapay.data.repository.BillingRepository
import com.arbdev.sapay.ui.screens.formatIndonesianDate
import com.arbdev.sapay.ui.util.StatusBarNotificationUtil
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class BillReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val context = applicationContext
            if (!StatusBarNotificationUtil.hasNotificationPermission(context)) {
                return Result.success()
            }

            val preferences = AppPreferences(context)
            val authToken = preferences.authToken.first()
            val savedCustomerId = preferences.savedCustomerId.first()

            // 1. Cek riwayat tagihan jika pengguna login dengan akun terdaftar
            if (authToken.isNotBlank()) {
                val authRepository = AuthRepository(ApiClient.starliteApi)
                val historyResult = authRepository.getHistory(authToken)
                val unpaid = historyResult.getOrNull()?.firstOrNull { it.isPaid == false }

                if (unpaid != null) {
                    val savedName = preferences.savedCustomerName.first().ifBlank { "Pelanggan Starlite" }
                    val customerId = savedCustomerId.ifBlank { preferences.userPhone.first() }
                    val packageName = unpaid.regionalPackage?.packageInfo?.name
                        ?: unpaid.regionalPackage?.name
                        ?: "Paket Internet WiFi"
                    val amount = unpaid.amount ?: unpaid.regionalPackage?.price ?: 0.0
                    val dueDate = formatIndonesianDate(unpaid.dueDate, withTime = false)

                    StatusBarNotificationUtil.showBillDueNotification(
                        context = context,
                        customerName = savedName,
                        customerId = customerId,
                        amount = amount,
                        dueDate = dueDate,
                        packageName = packageName
                    )
                    return Result.success()
                }
            }

            // 2. Cek tagihan berkala berdasarkan ID pelanggan yang pernah disimpan / dicari
            if (savedCustomerId.isNotBlank()) {
                val billingRepository = BillingRepository(ApiClient.starliteApi)
                val billResult = billingRepository.checkBill(savedCustomerId)
                val customer = billResult.getOrNull()?.customer

                if (customer != null) {
                    val pkg = customer.regionalPackage ?: customer.activeRegionalPackage
                    val expireDateStr = customer.activePackageExpireDate ?: customer.settlementDate
                    val isDueOrExpiring = isDateDueOrExpiringSoon(expireDateStr) ||
                            customer.status.equals("inactive", ignoreCase = true) ||
                            customer.status.equals("expired", ignoreCase = true) ||
                            customer.status.equals("suspend", ignoreCase = true)

                    if (isDueOrExpiring && pkg != null) {
                        val customerName = customer.name ?: preferences.savedCustomerName.first().ifBlank { "Pelanggan Starlite" }
                        val pkgName = pkg.packageInfo?.name ?: pkg.name ?: "Paket Internet WiFi"
                        val amount = pkg.price ?: 0.0
                        val dueDate = formatIndonesianDate(expireDateStr, withTime = false)

                        StatusBarNotificationUtil.showBillDueNotification(
                            context = context,
                            customerName = customerName,
                            customerId = customer.customerId ?: savedCustomerId,
                            amount = amount,
                            dueDate = dueDate,
                            packageName = pkgName
                        )
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun isDateDueOrExpiringSoon(rawDateStr: String?): Boolean {
        if (rawDateStr.isNullOrBlank()) return false
        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            },
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            },
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US),
            SimpleDateFormat("yyyy-MM-dd", Locale.US)
        )

        for (sdf in formats) {
            try {
                val parsed = sdf.parse(rawDateStr)
                if (parsed != null) {
                    val now = System.currentTimeMillis()
                    // Pengingat aktif jika jatuh tempo dalam 3 hari ke depan atau sudah lewat jatuh tempo
                    val threeDaysInMillis = 3L * 24 * 60 * 60 * 1000
                    return parsed.time - now <= threeDaysInMillis
                }
            } catch (_: Exception) {
            }
        }
        return false
    }

    companion object {
        const val WORK_NAME = "starlite_periodic_bill_reminder"

        fun schedulePeriodicReminder(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<BillReminderWorker>(
                12L,
                TimeUnit.HOURS,
                2L,
                TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        }
    }
}
