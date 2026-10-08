package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.PaperWidthProfile
import com.example.data.model.PrinterConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.printerDataStore: DataStore<Preferences> by preferencesDataStore(name = "printer_settings")

class PrinterSettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val SELECTED_DEVICE_ADDRESS = stringPreferencesKey("selected_device_address")
        val SELECTED_DEVICE_NAME = stringPreferencesKey("selected_device_name")
        val PAPER_WIDTH_PROFILE = stringPreferencesKey("paper_width_profile")
        val STORE_NAME = stringPreferencesKey("store_name")
        val STORE_ADDRESS = stringPreferencesKey("store_address")
        val STORE_PHONE = stringPreferencesKey("store_phone")
        val RECEIPT_FOOTER = stringPreferencesKey("receipt_footer")
        val CHARS_PER_LINE = intPreferencesKey("chars_per_line")
    }

    val printerConfigFlow: Flow<PrinterConfig> = context.printerDataStore.data.map { preferences ->
        val profileId = preferences[PreferencesKeys.PAPER_WIDTH_PROFILE]
        val profile = PaperWidthProfile.fromId(profileId)
        val charsOverride = preferences[PreferencesKeys.CHARS_PER_LINE] ?: profile.charsPerLine

        PrinterConfig(
            selectedDeviceAddress = preferences[PreferencesKeys.SELECTED_DEVICE_ADDRESS],
            selectedDeviceName = preferences[PreferencesKeys.SELECTED_DEVICE_NAME],
            paperWidthProfile = profile,
            storeName = preferences[PreferencesKeys.STORE_NAME] ?: "TokoKu",
            storeAddress = preferences[PreferencesKeys.STORE_ADDRESS] ?: "",
            storePhone = preferences[PreferencesKeys.STORE_PHONE] ?: "",
            receiptFooter = preferences[PreferencesKeys.RECEIPT_FOOTER] ?: "Terima kasih",
            charsPerLineOverride = charsOverride
        )
    }

    suspend fun saveSelectedPrinter(name: String, address: String) {
        context.printerDataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_DEVICE_NAME] = name
            preferences[PreferencesKeys.SELECTED_DEVICE_ADDRESS] = address
        }
    }

    suspend fun clearSelectedPrinter() {
        context.printerDataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.SELECTED_DEVICE_NAME)
            preferences.remove(PreferencesKeys.SELECTED_DEVICE_ADDRESS)
        }
    }

    suspend fun updatePaperProfile(profile: PaperWidthProfile) {
        context.printerDataStore.edit { preferences ->
            preferences[PreferencesKeys.PAPER_WIDTH_PROFILE] = profile.id
            preferences[PreferencesKeys.CHARS_PER_LINE] = profile.charsPerLine
        }
    }

    suspend fun updateStoreInfo(
        storeName: String,
        storeAddress: String,
        storePhone: String,
        receiptFooter: String
    ) {
        context.printerDataStore.edit { preferences ->
            preferences[PreferencesKeys.STORE_NAME] = storeName
            preferences[PreferencesKeys.STORE_ADDRESS] = storeAddress
            preferences[PreferencesKeys.STORE_PHONE] = storePhone
            preferences[PreferencesKeys.RECEIPT_FOOTER] = receiptFooter
        }
    }

    suspend fun updateCharsPerLine(chars: Int) {
        context.printerDataStore.edit { preferences ->
            preferences[PreferencesKeys.CHARS_PER_LINE] = chars
        }
    }
}
