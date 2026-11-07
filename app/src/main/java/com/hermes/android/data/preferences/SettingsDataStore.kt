package com.hermes.android.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hermes.android.data.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.io.IOException
import kotlinx.serialization.encodeToString

private val Context.hermesDataStore: DataStore<Preferences> by preferencesDataStore(name = "hermes_settings")

object SettingsKeys {
    val SETTINGS_JSON = stringPreferencesKey("settings_json")
}

class SettingsDataStore(private val dataStore: DataStore<Preferences>) {
    private val json = Json { ignoreUnknownKeys = true }

    val settingsFlow: Flow<AppSettings> = dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            val jsonString = prefs[SettingsKeys.SETTINGS_JSON] ?: ""
            if (jsonString.isBlank()) {
                AppSettings()
            } else {
                try {
                    json.decodeFromString(jsonString)
                } catch (e: Exception) {
                    AppSettings()
                }
            }
        }
        .distinctUntilChanged()

    suspend fun saveSettings(settings: AppSettings): Boolean {
        val jsonString = json.encodeToString(settings)
        return try {
            dataStore.edit { prefs ->
                prefs[SettingsKeys.SETTINGS_JSON] = jsonString
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun observeSettings(): Flow<AppSettings> = settingsFlow

    companion object {
        fun create(context: Context): SettingsDataStore {
            return SettingsDataStore(context.applicationContext.hermesDataStore)
        }
    }
}
