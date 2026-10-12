package com.arbdev.sapay.ui.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.NumberFormat
import java.util.Locale

object QrisDownloadUtil {

    /**
     * Membuat Bitmap Poster QRIS yang lengkap dengan Header SaPay/Starlite, QR Code, Nama Pelanggan, dan Total Tagihan.
     */
    fun createQrisPosterBitmap(
        qrBitmap: Bitmap,
        customerName: String,
        customerId: String,
        amount: Double,
        trxCode: String
    ): Bitmap {
        val width = 720
        val height = 1000
        val poster = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(poster)

        // 1. Background Putih Bersih
        canvas.drawColor(Color.WHITE)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
        }

        // 2. Header Box Biru
        val headerPaint = Paint().apply {
            color = Color.parseColor("#084C94")
        }
        canvas.drawRect(0f, 0f, width.toFloat(), 140f, headerPaint)

        // Teks Header
        paint.color = Color.WHITE
        paint.textSize = 34f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("STARLITE QRIS PAYMENT", width / 2f, 65f, paint)

        paint.textSize = 22f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Pindai QR ini dari BCA, BRI, Mandiri, Dana, GoPay, OVO, dll", width / 2f, 105f, paint)

        // 3. Info Pelanggan
        paint.color = Color.parseColor("#1E293B")
        paint.textSize = 28f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val displayName = if (customerName.isNotBlank()) customerName else "Pelanggan Starlite"
        canvas.drawText(displayName, width / 2f, 195f, paint)

        paint.color = Color.parseColor("#64748B")
        paint.textSize = 22f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("ID: $customerId  |  Trx: $trxCode", width / 2f, 230f, paint)

        // 4. Gambar QR Code di Tengah
        val qrSize = 460
        val qrLeft = (width - qrSize) / 2
        val qrTop = 270
        val srcRect = Rect(0, 0, qrBitmap.width, qrBitmap.height)
        val destRect = Rect(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize)
        canvas.drawBitmap(qrBitmap, srcRect, destRect, null)

        // 5. Border Tipis Sekeliling QR
        val borderPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = Color.parseColor("#E2E8F0")
        }
        canvas.drawRect(qrLeft.toFloat(), qrTop.toFloat(), (qrLeft + qrSize).toFloat(), (qrTop + qrSize).toFloat(), borderPaint)

        // 6. Total Tagihan di Bawah
        paint.color = Color.parseColor("#64748B")
        paint.textSize = 22f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("TOTAL TAGIHAN", width / 2f, 785f, paint)

        val formattedAmount = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount)
        paint.color = Color.parseColor("#084C94")
        paint.textSize = 44f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(formattedAmount, width / 2f, 840f, paint)

        // 7. Footer Petunjuk
        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 20f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Setelah transfer, tagihan internet WiFi otomatis aktif kembali.", width / 2f, 910f, paint)
        canvas.drawText("SaPay • Starlite Indonesia Payment Hub", width / 2f, 945f, paint)

        return poster
    }

    /**
     * Menyimpan Bitmap QRIS ke Galeri Pengguna (Pictures / SaPay)
     */
    fun saveQrisToGallery(
        context: Context,
        bitmap: Bitmap,
        trxCode: String
    ): Result<Uri> {
        val fileName = "QRIS_Starlite_${trxCode.ifBlank { System.currentTimeMillis() }}.png"

        return try {
            val uri: Uri?
            val outputStream: OutputStream?

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SaPay")
                }
                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return Result.failure(Exception("Gagal membuat entri penyimpanan media"))
                uri = imageUri
                outputStream = resolver.openOutputStream(imageUri)
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "SaPay")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, fileName)
                uri = Uri.fromFile(file)
                outputStream = FileOutputStream(file)
            }

            outputStream?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            } ?: return Result.failure(Exception("Gagal membuka aliran penyimpanan berkas"))

            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(Exception("Gagal menyimpan gambar ke galeri: ${e.message}", e))
        }
    }

    /**
     * Membagikan Gambar QRIS langsung ke WhatsApp / Aplikasi Lain
     */
    fun shareQrisImage(
        context: Context,
        bitmap: Bitmap,
        captionText: String
    ): Result<Unit> {
        return try {
            val cacheFile = File(context.cacheDir, "shared_qris.png")
            FileOutputStream(cacheFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                cacheFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, captionText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Bagikan QRIS Pembayaran")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Gagal membagikan QRIS: ${e.message}", e))
        }
    }
}
