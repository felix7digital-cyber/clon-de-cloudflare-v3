package com.cfadmin.pro.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cf_prefs")

class UserPrefs(private val context: Context) {
    private val KEY_DARK = booleanPreferencesKey("dark_theme")
    private val KEY_RECENT = stringPreferencesKey("recent")
    private val KEY_PINNED = stringPreferencesKey("pinned")
    private val KEY_DOMAIN = stringPreferencesKey("domain")

    private val safeData = context.dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }

    val darkTheme: Flow<Boolean> = safeData.map { it[KEY_DARK] ?: true }

    val recent: Flow<List<String>> = safeData.map {
        it[KEY_RECENT]?.split(",")?.filter { s -> s.isNotBlank() } ?: emptyList()
    }

    val pinned: Flow<List<String>> = safeData.map {
        it[KEY_PINNED]?.split(",")?.filter { s -> s.isNotBlank() }
            ?: listOf("overview", "pages")
    }

    val domain: Flow<String> = safeData.map {
        it[KEY_DOMAIN] ?: "midominio.com"
    }

    suspend fun setDarkTheme(dark: Boolean) {
        try { context.dataStore.edit { it[KEY_DARK] = dark } } catch (_: Exception) {}
    }
    suspend fun setRecent(list: List<String>) {
        try { context.dataStore.edit { it[KEY_RECENT] = list.joinToString(",") } } catch (_: Exception) {}
    }
    suspend fun setPinned(list: List<String>) {
        try { context.dataStore.edit { it[KEY_PINNED] = list.joinToString(",") } } catch (_: Exception) {}
    }
    suspend fun setDomain(domain: String) {
        try { context.dataStore.edit { it[KEY_DOMAIN] = domain } } catch (_: Exception) {}
    }
}
