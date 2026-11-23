package com.example.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "api_manager_theme_prefs"
)

interface ThemePreferencesStore {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}

class DataStoreThemePreferencesStore(
    private val context: Context
) : ThemePreferencesStore {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    override val themeMode: Flow<ThemeMode> = context.themeDataStore.data.map { prefs ->
        ThemeMode.fromStored(prefs[Keys.THEME_MODE])
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }
}

class InMemoryThemePreferencesStore : ThemePreferencesStore {
    private val state = kotlinx.coroutines.flow.MutableStateFlow(ThemeMode.AUTO)
    override val themeMode: Flow<ThemeMode> = state
    override suspend fun setThemeMode(mode: ThemeMode) {
        state.value = mode
    }
}
