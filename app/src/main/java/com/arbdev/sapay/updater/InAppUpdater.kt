package com.arbdev.sapay.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

class InAppUpdater(private val context: Context) {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    /**
     * Memeriksa apakah berkas APK untuk versi ini sudah pernah diunduh secara utuh sebelumnya.
     * Mengembalikan File jika valid (> 5 MB), atau null jika belum ada / corrupt.
     */
    fun getExistingDownloadedApk(versionTag: String): File? {
        val cleanTag = versionTag.trim().removePrefix("v")
        val fileName = "sapay-$cleanTag.apk"
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val targetFile = File(dir, fileName)

        // Verifikasi file ada dan ukuran minimal 5MB (APK rilis SaPay biasanya ~13MB)
        return if (targetFile.exists() && targetFile.length() > 5 * 1024 * 1024) {
            targetFile
        } else {
            null
        }
    }

    /**
     * Mengunduh berkas APK dengan streaming coroutine murni tanpa notifikasi bawaan OS.
     * Dilengkapi pemantau progress (0.0f - 1.0f) dan pembersihan file sementara jika gagal.
     */
    suspend fun downloadApk(
        apkUrl: String,
        versionTag: String,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val cleanTag = versionTag.trim().removePrefix("v")
        val finalFileName = "sapay-$cleanTag.apk"
        val tempFileName = "sapay-$cleanTag.apk.tmp"

        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val finalFile = File(dir, finalFileName)
        val tempFile = File(dir, tempFileName)

        // 1. Cek apakah sudah terunduh sebelumnya (Cegah Dobel Download)
        if (finalFile.exists() && finalFile.length() > 5 * 1024 * 1024) {
            onProgress(1.0f)
            return@withContext Result.success(finalFile)
        }

        // 2. Bersihkan sisa unduhan sementara yang lama jika ada
        if (tempFile.exists()) {
            tempFile.delete()
        }

        try {
            val request = Request.Builder()
                .url(apkUrl)
                .addHeader("User-Agent", "SaPay-Android-App")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful || response.body == null) {
                return@withContext Result.failure(
                    Exception("Gagal mengunduh berkas dari server (HTTP ${response.code})")
                )
            }

            val body = response.body!!
            val contentLength = body.contentLength()
            var inputStream: InputStream? = null
            var outputStream: FileOutputStream? = null

            try {
                inputStream = body.byteStream()
                outputStream = FileOutputStream(tempFile)

                val buffer = ByteArray(8 * 1024)
                var bytesRead: Int
                var totalBytesRead: Long = 0

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead

                    if (contentLength > 0) {
                        val progress = (totalBytesRead.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                        onProgress(progress)
                    }
                }

                outputStream.flush()
            } finally {
                inputStream?.close()
                outputStream?.close()
            }

            // 3. Verifikasi integritas unduhan
            if (tempFile.length() < 1024 * 1024) {
                tempFile.delete()
                return@withContext Result.failure(
                    Exception("Berkas pembaruan tidak lengkap atau rusak.")
                )
            }

            // 4. Rename temp file ke file final
            if (finalFile.exists()) {
                finalFile.delete()
            }
            val renamed = tempFile.renameTo(finalFile)
            val readyFile = if (renamed) finalFile else tempFile

            onProgress(1.0f)
            Result.success(readyFile)
        } catch (e: Exception) {
            // Hapus berkas parsial jika gagal agar tidak corrupt
            if (tempFile.exists()) {
                tempFile.delete()
            }
            Result.failure(Exception("Gagal mengunduh pembaruan: ${e.localizedMessage ?: "Gangguan koneksi"}", e))
        }
    }

    /**
     * Membuka installer Android untuk memasang berkas APK.
     */
    fun installApk(file: File, onError: (String) -> Unit = {}) {
        if (!file.exists()) {
            onError("Berkas instalasi tidak ditemukan.")
            return
        }

        // Cek izin instalasi aplikasi tidak dikenal di Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val packageManager = context.packageManager
            if (!packageManager.canRequestPackageInstalls()) {
                val manageIntent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(manageIntent)
                onError("Harap izinkan pemasangan aplikasi untuk melanjutkan.")
                return
            }
        }

        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            onError("Gagal membuka installer APK: ${e.message}")
        }
    }
}
