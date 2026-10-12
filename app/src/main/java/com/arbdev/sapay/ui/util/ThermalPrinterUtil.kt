package com.arbdev.sapay.ui.util

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import com.arbdev.sapay.data.model.ReceiptData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

object ThermalPrinterUtil {

    // Standard Serial Port Profile (SPP) UUID untuk Printer Bluetooth
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // Lebar kolom standar printer thermal 58mm
    private const val LINE_WIDTH_58MM = 32

    private fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount)
    }

    /**
     * Mengambil daftar perangkat Bluetooth yang sudah terpasang (paired/bonded).
     */
    @SuppressLint("MissingPermission")
    fun getPairedPrinters(context: Context): List<BluetoothDevice> {
        return try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
            if (!adapter.isEnabled) return emptyList()

            adapter.bondedDevices.filter { device ->
                // Filter perangkat bertipe printer, imaging, atau nama yang mengandung "printer", "pos", "thermal", "bt"
                val deviceClass = device.bluetoothClass?.majorDeviceClass ?: 0
                val name = device.name?.lowercase() ?: ""
                deviceClass == 1536 || deviceClass == 1664 || // Imaging / Peripheral
                        name.contains("print") || name.contains("pos") ||
                        name.contains("thermal") || name.contains("rpp") ||
                        name.contains("mpt") || name.contains("bt") || name.contains("58") || name.contains("80")
            }.ifEmpty {
                // Jika tidak ada filter yang cocok, kembalikan semua perangkat terpasang agar pengguna bebas memilih
                adapter.bondedDevices.toList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Memeriksa apakah Bluetooth pada perangkat aktif.
     */
    fun isBluetoothEnabled(context: Context): Boolean {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter() ?: return false
        return adapter.isEnabled
    }

    /**
     * Mengirim instruksi cetak nota ESC/POS ke printer Bluetooth yang dipilih.
     */
    @SuppressLint("MissingPermission")
    suspend fun printReceipt(
        context: Context,
        device: BluetoothDevice,
        data: ReceiptData
    ): Result<Unit> = withContext(Dispatchers.IO) {
        var socket: BluetoothSocket? = null
        var outputStream: OutputStream? = null

        try {
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            outputStream = socket.outputStream

            // 1. Initialize Printer (ESC @)
            outputStream.write(byteArrayOf(0x1B, 0x40))

            // 2. Set Karakter Code Table Indonesia/Standard (ESC t 0)
            outputStream.write(byteArrayOf(0x1B, 0x74, 0x00))

            // 3. Cetak Logo Toko jika ada
            if (!data.storeLogoPath.isNullOrBlank()) {
                val logoBitmap = loadLogoBitmap(context, data.storeLogoPath)
                if (logoBitmap != null) {
                    printEscPosBitmap(outputStream, logoBitmap)
                    outputStream.write("\n".toByteArray())
                }
            }

            // 4. Header Toko (Center, Bold, Double Size)
            setAlign(outputStream, 1) // Center
            setTextSize(outputStream, doubleWidth = true, doubleHeight = true)
            setBold(outputStream, true)
            outputStream.write("${data.storeName.uppercase()}\n".toByteArray())

            setTextSize(outputStream, doubleWidth = false, doubleHeight = false)
            setBold(outputStream, false)
            if (data.storeAddress.isNotBlank()) {
                outputStream.write("${data.storeAddress}\n".toByteArray())
            }
            if (data.storePhone.isNotBlank() && data.storePhone != "-") {
                outputStream.write("Telp: ${data.storePhone}\n".toByteArray())
            }

            printDivider(outputStream, "=")

            // 5. Judul Bukti
            setBold(outputStream, true)
            outputStream.write("BUKTI PEMBAYARAN WIFI\n".toByteArray())
            setBold(outputStream, false)

            printDivider(outputStream, "-")

            // 6. Data Transaksi (Align Left)
            setAlign(outputStream, 0)
            printTwoColumns(outputStream, "Waktu", data.paymentDate)
            printTwoColumns(outputStream, "No. Trx", data.trxCode)
            printTwoColumns(outputStream, "Metode", data.paymentMethod)

            printDivider(outputStream, "-")

            // 7. Data Pelanggan
            printTwoColumns(outputStream, "ID Pelanggan", data.customerId)
            printTwoColumns(outputStream, "Nama", data.customerName.take(18))
            printTwoColumns(outputStream, "Paket WiFi", data.packageName.take(18))
            if (data.billingPeriod.isNotBlank()) {
                printTwoColumns(outputStream, "Periode", data.billingPeriod)
            }

            printDivider(outputStream, "-")

            // 8. Rincian Biaya
            printTwoColumns(outputStream, "Biaya Paket", formatCurrency(data.packageAmount))
            if (data.channelFee > 0) {
                printTwoColumns(outputStream, "Biaya Saluran", formatCurrency(data.channelFee))
            }
            if (data.customAdminFee > 0) {
                printTwoColumns(outputStream, "Biaya Admin", formatCurrency(data.customAdminFee))
            }

            printDivider(outputStream, "=")

            // 9. Total Bayar (Bold, Double Size)
            val total = data.packageAmount + data.channelFee + data.customAdminFee
            setBold(outputStream, true)
            setTextSize(outputStream, doubleWidth = true, doubleHeight = false)
            printTwoColumns(outputStream, "TOTAL", formatCurrency(total), lineWidth = 24)
            setTextSize(outputStream, doubleWidth = false, doubleHeight = false)

            printDivider(outputStream, "=")

            // 10. Status Lunas
            setAlign(outputStream, 1) // Center
            setBold(outputStream, true)
            outputStream.write("\n*** L U N A S ***\n\n".toByteArray())
            setBold(outputStream, false)

            // 11. Footer
            if (data.footerMessage.isNotBlank()) {
                outputStream.write("${data.footerMessage}\n".toByteArray())
            }
            outputStream.write("SaPay • Starlite Indonesia\n".toByteArray())

            // 12. Feed paper 4 baris agar mudah disobek
            outputStream.write(byteArrayOf(0x1B, 0x64, 0x04))

            outputStream.flush()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Gagal mencetak: ${e.message ?: "Printer tidak terhubung"}", e))
        } finally {
            try {
                outputStream?.close()
                socket?.close()
            } catch (_: Exception) {}
        }
    }

    private fun setAlign(out: OutputStream, align: Int) {
        // align: 0 = Kiri, 1 = Tengah, 2 = Kanan
        out.write(byteArrayOf(0x1B, 0x61, align.toByte()))
    }

    private fun setBold(out: OutputStream, bold: Boolean) {
        out.write(byteArrayOf(0x1B, 0x45, if (bold) 1 else 0))
    }

    private fun setTextSize(out: OutputStream, doubleWidth: Boolean, doubleHeight: Boolean) {
        var n = 0
        if (doubleHeight) n = n or 0x01
        if (doubleWidth) n = n or 0x10
        out.write(byteArrayOf(0x1D, 0x21, n.toByte()))
    }

    private fun printDivider(out: OutputStream, char: String, length: Int = LINE_WIDTH_58MM) {
        val line = char.repeat(length) + "\n"
        out.write(line.toByteArray())
    }

    private fun printTwoColumns(
        out: OutputStream,
        left: String,
        right: String,
        lineWidth: Int = LINE_WIDTH_58MM
    ) {
        val maxLeft = lineWidth - right.length - 1
        val cleanLeft = if (left.length > maxLeft) left.take(maxLeft) else left
        val spacesCount = (lineWidth - cleanLeft.length - right.length).coerceAtLeast(1)
        val spaces = " ".repeat(spacesCount)
        val line = "$cleanLeft$spaces$right\n"
        out.write(line.toByteArray())
    }

    private fun loadLogoBitmap(context: Context, path: String): Bitmap? {
        return try {
            val file = File(path)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                val uri = Uri.parse(path)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Mengubah Bitmap logo menjadi format Raster Bit Image ESC/POS (GS v 0).
     */
    private fun printEscPosBitmap(out: OutputStream, src: Bitmap) {
        // Batasi lebar gambar maksimal 384 pixel (standar thermal 58mm)
        val maxWidth = 384
        val scale = if (src.width > maxWidth) maxWidth.toFloat() / src.width.toFloat() else 1.0f
        val targetW = (src.width * scale).toInt()
        val targetH = (src.height * scale).toInt()

        val scaled = Bitmap.createScaledBitmap(src, targetW, targetH, true)
        val widthBytes = (targetW + 7) / 8

        // Perintah ESC/POS: GS v 0 m xL xH yL yH data...
        out.write(byteArrayOf(
            0x1D, 0x76, 0x30, 0x00,
            (widthBytes and 0xFF).toByte(),
            ((widthBytes shr 8) and 0xFF).toByte(),
            (targetH and 0xFF).toByte(),
            ((targetH shr 8) and 0xFF).toByte()
        ))

        val data = ByteArray(widthBytes * targetH)
        var offset = 0

        for (y in 0 until targetH) {
            for (xByte in 0 until widthBytes) {
                var byteVal = 0
                for (b in 0 until 8) {
                    val x = xByte * 8 + b
                    if (x < targetW) {
                        val pixel = scaled.getPixel(x, y)
                        val r = Color.red(pixel)
                        val g = Color.green(pixel)
                        val bl = Color.blue(pixel)
                        val lum = (r * 0.299 + g * 0.587 + bl * 0.114).toInt()
                        if (lum < 160) { // Titik hitam jika lebih gelap dari ambang batas
                            byteVal = byteVal or (1 shl (7 - b))
                        }
                    }
                }
                data[offset++] = byteVal.toByte()
            }
        }

        out.write(data)
    }
}
