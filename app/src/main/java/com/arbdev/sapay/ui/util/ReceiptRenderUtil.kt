package com.arbdev.sapay.ui.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import com.arbdev.sapay.data.model.ReceiptData
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale

object ReceiptRenderUtil {

    private fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount)
    }

    /**
     * Menghasilkan teks struk WhatsApp yang terformat sangat rapi dan monospaced.
     */
    fun formatWhatsAppReceiptText(data: ReceiptData): String {
        val total = data.packageAmount + data.channelFee + data.customAdminFee
        val sb = StringBuilder()

        sb.append("🧾 *BUKTI PEMBAYARAN TAGIHAN INTERNET*\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("🏪 *${data.storeName.uppercase()}*\n")
        if (data.storeAddress.isNotBlank()) sb.append("📍 ${data.storeAddress}\n")
        if (data.storePhone.isNotBlank() && data.storePhone != "-") sb.append("📞 Telp/WA: ${data.storePhone}\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("🗓️ *Tanggal*     : ${data.paymentDate}\n")
        sb.append("🔖 *No. Trx*      : ${data.trxCode}\n")
        sb.append("💳 *Metode*      : ${data.paymentMethod}\n")
        sb.append("✅ *Status*       : *LUNAS*\n")
        sb.append("────────────────────────────\n")
        sb.append("👤 *ID Pelanggan*: `${data.customerId}`\n")
        sb.append("🏷️ *Nama*         : *${data.customerName}*\n")
        sb.append("📦 *Paket WiFi*   : ${data.packageName}\n")
        if (data.billingPeriod.isNotBlank()) sb.append("📅 *Periode*      : ${data.billingPeriod}\n")
        sb.append("────────────────────────────\n")
        sb.append("💵 *Biaya Paket*  : ${formatCurrency(data.packageAmount)}\n")
        if (data.channelFee > 0) {
            sb.append("⚡ *Biaya Saluran* : ${formatCurrency(data.channelFee)}\n")
        }
        if (data.customAdminFee > 0) {
            sb.append("🛠️ *Biaya Admin*   : ${formatCurrency(data.customAdminFee)}\n")
        }
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("💰 *TOTAL BAYAR*  : *${formatCurrency(total)}*\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n")
        sb.append("_\"${data.footerMessage}\"_\n\n")
        sb.append("⚡ _Diterbitkan oleh SaPay Internet Starlite_")

        return sb.toString()
    }

    /**
     * Membagikan teks nota langsung ke WhatsApp / aplikasi lain.
     */
    fun shareReceiptAsText(context: Context, data: ReceiptData) {
        val text = formatWhatsAppReceiptText(data)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            // Coba target langsung ke WhatsApp jika ada, atau fallback ke chooser
            setPackage("com.whatsapp")
        }

        try {
            sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(sendIntent)
        } catch (_: Exception) {
            // Fallback ke sistem share chooser jika WhatsApp tidak terpasang
            val chooser = Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }, "Bagikan Nota Pembayaran")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
    }

    /**
     * Merender Bitmap struk nota kasir thermal yang elegan (berwarna putih kertas struk kasir).
     */
    fun renderReceiptBitmap(context: Context, data: ReceiptData): Bitmap {
        val width = 640
        var estimatedHeight = 960

        // Muat logo jika ada
        var logoBitmap: Bitmap? = null
        if (!data.storeLogoPath.isNullOrBlank()) {
            try {
                val file = File(data.storeLogoPath)
                if (file.exists()) {
                    logoBitmap = BitmapFactory.decodeFile(file.absolutePath)
                } else {
                    val uri = Uri.parse(data.storeLogoPath)
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        logoBitmap = BitmapFactory.decodeStream(stream)
                    }
                }
            } catch (_: Exception) {}
        }

        if (logoBitmap != null) {
            estimatedHeight += 120
        }

        val bitmap = Bitmap.createBitmap(width, estimatedHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Latar Belakang Kertas Putih Bersih
        canvas.drawColor(Color.WHITE)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            typeface = Typeface.MONOSPACE
        }

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            strokeWidth = 2f
            pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f)
        }

        var y = 40f

        // 1. Gambar Logo jika ada
        if (logoBitmap != null) {
            val maxLogoSize = 100
            val aspect = logoBitmap!!.width.toFloat() / logoBitmap!!.height.toFloat()
            val logoW = if (aspect >= 1) maxLogoSize else (maxLogoSize * aspect).toInt()
            val logoH = if (aspect >= 1) (maxLogoSize / aspect).toInt() else maxLogoSize
            val logoLeft = (width - logoW) / 2
            val src = Rect(0, 0, logoBitmap!!.width, logoBitmap!!.height)
            val dest = Rect(logoLeft, y.toInt(), logoLeft + logoW, (y + logoH).toInt())
            canvas.drawBitmap(logoBitmap!!, src, dest, null)
            y += logoH + 20f
        }

        // 2. Header Toko
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 28f
        textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        canvas.drawText(data.storeName.uppercase(), width / 2f, y, textPaint)
        y += 34f

        textPaint.textSize = 20f
        textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        textPaint.color = Color.parseColor("#475569")
        if (data.storeAddress.isNotBlank()) {
            canvas.drawText(data.storeAddress, width / 2f, y, textPaint)
            y += 28f
        }
        if (data.storePhone.isNotBlank() && data.storePhone != "-") {
            canvas.drawText("Telp: ${data.storePhone}", width / 2f, y, textPaint)
            y += 28f
        }

        y += 10f
        canvas.drawLine(30f, y, (width - 30).toFloat(), y, linePaint)
        y += 30f

        // 3. Judul Bukti & Tanggal
        textPaint.color = Color.parseColor("#084C94")
        textPaint.textSize = 22f
        textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        canvas.drawText("BUKTI PEMBAYARAN WIFI", width / 2f, y, textPaint)
        y += 32f

        textPaint.color = Color.parseColor("#1E293B")
        textPaint.textSize = 19f
        textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)

        fun drawRow(label: String, value: String) {
            textPaint.textAlign = Paint.Align.LEFT
            canvas.drawText(label, 36f, y, textPaint)
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(value, (width - 36).toFloat(), y, textPaint)
            y += 30f
        }

        drawRow("Waktu", data.paymentDate)
        drawRow("No. Trx", data.trxCode)
        drawRow("Metode", data.paymentMethod)

        y += 10f
        canvas.drawLine(30f, y, (width - 30).toFloat(), y, linePaint)
        y += 30f

        // 4. Data Pelanggan
        drawRow("ID Pelanggan", data.customerId)
        drawRow("Nama", if (data.customerName.length > 20) data.customerName.take(18) + ".." else data.customerName)
        drawRow("Paket", if (data.packageName.length > 20) data.packageName.take(18) + ".." else data.packageName)
        if (data.billingPeriod.isNotBlank()) {
            drawRow("Periode", data.billingPeriod)
        }

        y += 10f
        canvas.drawLine(30f, y, (width - 30).toFloat(), y, linePaint)
        y += 30f

        // 5. Rincian Biaya
        drawRow("Biaya Paket", formatCurrency(data.packageAmount))
        if (data.channelFee > 0) {
            drawRow("Biaya Layanan", formatCurrency(data.channelFee))
        }
        if (data.customAdminFee > 0) {
            drawRow("Biaya Admin", formatCurrency(data.customAdminFee))
        }

        y += 10f
        val solidPaint = Paint().apply {
            color = Color.parseColor("#084C94")
            strokeWidth = 3f
        }
        canvas.drawLine(30f, y, (width - 30).toFloat(), y, solidPaint)
        y += 36f

        // 6. TOTAL BAYAR (Besar & Tebal)
        val total = data.packageAmount + data.channelFee + data.customAdminFee
        textPaint.textSize = 24f
        textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textPaint.color = Color.parseColor("#084C94")

        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("TOTAL BAYAR", 36f, y, textPaint)
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(formatCurrency(total), (width - 36).toFloat(), y, textPaint)
        y += 44f

        canvas.drawLine(30f, y, (width - 30).toFloat(), y, linePaint)
        y += 40f

        // 7. Stamp LUNAS
        val stampPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#166534")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val stampRect = Rect((width / 2) - 120, (y - 25).toInt(), (width / 2) + 120, (y + 15).toInt())
        canvas.drawRoundRect(
            stampRect.left.toFloat(),
            stampRect.top.toFloat(),
            stampRect.right.toFloat(),
            stampRect.bottom.toFloat(),
            12f,
            12f,
            stampPaint
        )

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 24f
        textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textPaint.color = Color.parseColor("#166534")
        canvas.drawText("★ L U N A S ★", width / 2f, y + 4f, textPaint)
        y += 56f

        // 8. Footer Pesan Toko
        textPaint.color = Color.parseColor("#64748B")
        textPaint.textSize = 18f
        textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        canvas.drawText(data.footerMessage, width / 2f, y, textPaint)
        y += 32f

        textPaint.color = Color.parseColor("#94A3B8")
        textPaint.textSize = 16f
        canvas.drawText("SaPay • Starlite Indonesia Payment", width / 2f, y, textPaint)
        y += 40f

        // Crop bitmap jika ukuran sebenarnya lebih kecil dari estimasi
        val finalHeight = y.toInt()
        val cropped = Bitmap.createBitmap(bitmap, 0, 0, width, finalHeight)
        return cropped
    }

    /**
     * Membagikan nota dalam format GAMBAR langsung ke WhatsApp / Chooser.
     */
    fun shareReceiptAsImage(context: Context, data: ReceiptData): Result<Unit> {
        return try {
            val bitmap = renderReceiptBitmap(context, data)
            val cacheFile = File(context.cacheDir, "nota_pembayaran_${data.trxCode.ifBlank { "wifi" }}.png")
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
                putExtra(Intent.EXTRA_TEXT, "Bukti Pembayaran WiFi Starlite - ${data.customerName} (${data.customerId})")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setPackage("com.whatsapp")
            }

            try {
                shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(shareIntent)
            } catch (_: Exception) {
                val chooser = Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, "Bukti Pembayaran WiFi Starlite - ${data.customerName}")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, "Bagikan Gambar Nota")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Gagal membagikan gambar nota: ${e.message}", e))
        }
    }
}
