package com.arbdev.sapay.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ReceiptData(
    val storeName: String = "SaPay / Starlite Outlet",
    val storeAddress: String = "Layanan Pembayaran Resmi",
    val storePhone: String = "-",
    val storeLogoPath: String? = null,
    val trxCode: String = "",
    val paymentDate: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val packageName: String = "",
    val billingPeriod: String = "",
    val packageAmount: Double = 0.0,
    val channelFee: Double = 0.0,
    val customAdminFee: Double = 2500.0,
    val totalAmount: Double = 0.0,
    val paymentMethod: String = "QRIS / Online",
    val status: String = "LUNAS",
    val footerMessage: String = "Terima kasih atas pembayaran Anda. Simpan struk ini sebagai bukti pembayaran yang sah."
)

data class StoreConfig(
    val storeName: String = "SaPay / Starlite Outlet",
    val storeAddress: String = "Layanan Pembayaran Resmi",
    val storePhone: String = "-",
    val storeFooter: String = "Terima kasih atas pembayaran Anda. Simpan struk ini sebagai bukti pembayaran yang sah.",
    val customAdminFee: Double = 2500.0,
    val storeLogoUri: String? = null,
    val printerMac: String? = null,
    val printerName: String? = null
)
