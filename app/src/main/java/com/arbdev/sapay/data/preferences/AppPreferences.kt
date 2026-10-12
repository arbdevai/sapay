package com.arbdev.sapay.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.arbdev.sapay.data.model.StoreConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sapay_settings")

class AppPreferences(private val context: Context) {

    companion object {
        val KEY_SAVED_CUSTOMER_ID = stringPreferencesKey("saved_customer_id")
        val KEY_SAVED_CUSTOMER_NAME = stringPreferencesKey("saved_customer_name")
        val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        val KEY_USER_PHONE = stringPreferencesKey("user_phone")

        // Konfigurasi Toko & Nota
        val KEY_STORE_NAME = stringPreferencesKey("store_name")
        val KEY_STORE_ADDRESS = stringPreferencesKey("store_address")
        val KEY_STORE_PHONE = stringPreferencesKey("store_phone")
        val KEY_STORE_FOOTER = stringPreferencesKey("store_footer")
        val KEY_STORE_ADMIN_FEE = doublePreferencesKey("store_admin_fee")
        val KEY_STORE_LOGO_URI = stringPreferencesKey("store_logo_uri")

        // Preferensi Printer Bluetooth
        val KEY_PRINTER_MAC = stringPreferencesKey("printer_mac")
        val KEY_PRINTER_NAME = stringPreferencesKey("printer_name")
    }

    val savedCustomerId: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_SAVED_CUSTOMER_ID] ?: ""
    }

    val savedCustomerName: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_SAVED_CUSTOMER_NAME] ?: ""
    }

    val authToken: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_AUTH_TOKEN] ?: ""
    }

    val userPhone: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_USER_PHONE] ?: ""
    }

    val storeConfig: Flow<StoreConfig> = context.dataStore.data.map { preferences ->
        StoreConfig(
            storeName = preferences[KEY_STORE_NAME] ?: "SaPay / Starlite Outlet",
            storeAddress = preferences[KEY_STORE_ADDRESS] ?: "Layanan Pembayaran Resmi",
            storePhone = preferences[KEY_STORE_PHONE] ?: "-",
            storeFooter = preferences[KEY_STORE_FOOTER] ?: "Terima kasih atas pembayaran Anda. Simpan struk ini sebagai bukti pembayaran yang sah.",
            customAdminFee = preferences[KEY_STORE_ADMIN_FEE] ?: 2500.0,
            storeLogoUri = preferences[KEY_STORE_LOGO_URI],
            printerMac = preferences[KEY_PRINTER_MAC],
            printerName = preferences[KEY_PRINTER_NAME]
        )
    }

    suspend fun saveCustomerInfo(customerId: String, customerName: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SAVED_CUSTOMER_ID] = customerId
            preferences[KEY_SAVED_CUSTOMER_NAME] = customerName
        }
    }

    suspend fun saveAuthToken(token: String, phone: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUTH_TOKEN] = token
            preferences[KEY_USER_PHONE] = phone
        }
    }

    suspend fun clearAuth() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_AUTH_TOKEN)
            preferences.remove(KEY_USER_PHONE)
        }
    }

    suspend fun saveStoreConfig(config: StoreConfig) {
        context.dataStore.edit { preferences ->
            preferences[KEY_STORE_NAME] = config.storeName
            preferences[KEY_STORE_ADDRESS] = config.storeAddress
            preferences[KEY_STORE_PHONE] = config.storePhone
            preferences[KEY_STORE_FOOTER] = config.storeFooter
            preferences[KEY_STORE_ADMIN_FEE] = config.customAdminFee
            if (config.storeLogoUri != null) {
                preferences[KEY_STORE_LOGO_URI] = config.storeLogoUri
            } else {
                preferences.remove(KEY_STORE_LOGO_URI)
            }
        }
    }

    suspend fun saveCustomAdminFee(fee: Double) {
        context.dataStore.edit { preferences ->
            preferences[KEY_STORE_ADMIN_FEE] = fee
        }
    }

    suspend fun savePrinterPreference(macAddress: String, name: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PRINTER_MAC] = macAddress
            preferences[KEY_PRINTER_NAME] = name
        }
    }
}
