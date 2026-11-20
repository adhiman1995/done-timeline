package com.donetimeline.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class Settings(
    val serverUrl: String = "",
    val token: String = "",
    /** Highest server revision this device has pulled. */
    val cursor: Long = 0,
    /** Epoch millis of the last successful sync, or 0. */
    val lastSyncAt: Long = 0,
) {
    val isConfigured get() = serverUrl.isNotBlank() && token.isNotBlank()
}

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {
    private object Keys {
        val serverUrl = stringPreferencesKey("server_url")
        val token = stringPreferencesKey("token")
        val cursor = longPreferencesKey("cursor")
        val lastSyncAt = longPreferencesKey("last_sync_at")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            serverUrl = p[Keys.serverUrl].orEmpty(),
            token = p[Keys.token].orEmpty(),
            cursor = p[Keys.cursor] ?: 0,
            lastSyncAt = p[Keys.lastSyncAt] ?: 0,
        )
    }

    suspend fun current(): Settings = settings.first()

    /** Saving a different server resets the cursor so the next sync pulls everything. */
    suspend fun saveConnection(serverUrl: String, token: String) {
        context.dataStore.edit { p ->
            val url = serverUrl.trim().trimEnd('/')
            if (p[Keys.serverUrl] != url) {
                p[Keys.cursor] = 0
                p[Keys.lastSyncAt] = 0
            }
            p[Keys.serverUrl] = url
            p[Keys.token] = token.trim()
        }
    }

    suspend fun recordSync(cursor: Long, at: Long) {
        context.dataStore.edit { p ->
            p[Keys.cursor] = cursor
            p[Keys.lastSyncAt] = at
        }
    }
}
