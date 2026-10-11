package com.arbdev.sapay.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sapay_settings")

class AppPreferences(private val context: Context) {

    companion object {
        val KEY_SAVED_CUSTOMER_ID = stringPreferencesKey("saved_customer_id")
        val KEY_SAVED_CUSTOMER_NAME = stringPreferencesKey("saved_customer_name")
        val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        val KEY_USER_PHONE = stringPreferencesKey("user_phone")
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
}
