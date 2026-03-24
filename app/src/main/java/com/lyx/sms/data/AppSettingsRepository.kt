package com.lyx.sms.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lyx.sms.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "my_sms_settings")

class AppSettingsRepository(private val context: Context) {

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        AppSettings(
            serverUrl = preferences[Keys.SERVER_URL].orEmpty(),
            token = preferences[Keys.TOKEN].orEmpty(),
            biometricEnabled = preferences[Keys.BIOMETRIC_ENABLED] ?: false
        )
    }

    suspend fun saveSettings(settings: AppSettings) {
        context.dataStore.edit { preferences ->
            preferences[Keys.SERVER_URL] = settings.serverUrl.trim()
            preferences[Keys.TOKEN] = settings.token.trim()
            preferences[Keys.BIOMETRIC_ENABLED] = settings.biometricEnabled
            preferences.remove(Keys.APP_LOCK_ENABLED)
            preferences.remove(Keys.PASSWORD_HASH)
        }
    }

    private object Keys {
        val SERVER_URL: Preferences.Key<String> = stringPreferencesKey("server_url")
        val TOKEN: Preferences.Key<String> = stringPreferencesKey("token")
        val APP_LOCK_ENABLED: Preferences.Key<Boolean> = booleanPreferencesKey("app_lock_enabled")
        val BIOMETRIC_ENABLED: Preferences.Key<Boolean> = booleanPreferencesKey("biometric_enabled")
        val PASSWORD_HASH: Preferences.Key<String> = stringPreferencesKey("password_hash")
    }
}
