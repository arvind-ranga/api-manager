package com.example.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "api_manager_theme_prefs"
)

interface ThemePreferencesStore {
    val themeMode: Flow<ThemeMode>
    val searchProvider: Flow<SearchProvider>
    val accentColor: Flow<Int?>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setSearchProvider(provider: SearchProvider)
    suspend fun setAccentColor(color: Int?)
}

class DataStoreThemePreferencesStore(
    private val context: Context
) : ThemePreferencesStore {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SEARCH_PROVIDER = stringPreferencesKey("search_provider")
        val ACCENT_COLOR = intPreferencesKey("accent_color")
    }

    override val themeMode: Flow<ThemeMode> = context.themeDataStore.data.map { prefs ->
        ThemeMode.fromStored(prefs[Keys.THEME_MODE])
    }

    override val searchProvider: Flow<SearchProvider> = context.themeDataStore.data.map { prefs ->
        SearchProvider.fromStored(prefs[Keys.SEARCH_PROVIDER])
    }

    override val accentColor: Flow<Int?> = context.themeDataStore.data.map { prefs ->
        prefs[Keys.ACCENT_COLOR]
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    override suspend fun setSearchProvider(provider: SearchProvider) {
        context.themeDataStore.edit { prefs ->
            prefs[Keys.SEARCH_PROVIDER] = provider.name
        }
    }

    override suspend fun setAccentColor(color: Int?) {
        context.themeDataStore.edit { prefs ->
            if (color == null) prefs.remove(Keys.ACCENT_COLOR) else prefs[Keys.ACCENT_COLOR] = color
        }
    }
}

class InMemoryThemePreferencesStore : ThemePreferencesStore {
    private val themeState = kotlinx.coroutines.flow.MutableStateFlow(ThemeMode.AUTO)
    private val searchState = kotlinx.coroutines.flow.MutableStateFlow(SearchProvider.GOOGLE_AI)
    private val accentState = kotlinx.coroutines.flow.MutableStateFlow<Int?>(null)

    override val themeMode: Flow<ThemeMode> = themeState
    override val searchProvider: Flow<SearchProvider> = searchState
    override val accentColor: Flow<Int?> = accentState

    override suspend fun setThemeMode(mode: ThemeMode) {
        themeState.value = mode
    }

    override suspend fun setSearchProvider(provider: SearchProvider) {
        searchState.value = provider
    }

    override suspend fun setAccentColor(color: Int?) {
        accentState.value = color
    }
}
