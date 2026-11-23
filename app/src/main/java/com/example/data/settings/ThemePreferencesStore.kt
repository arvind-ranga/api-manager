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
    val searchProvider: Flow<SearchProvider>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setSearchProvider(provider: SearchProvider)
}

class DataStoreThemePreferencesStore(
    private val context: Context
) : ThemePreferencesStore {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SEARCH_PROVIDER = stringPreferencesKey("search_provider")
    }

    override val themeMode: Flow<ThemeMode> = context.themeDataStore.data.map { prefs ->
        ThemeMode.fromStored(prefs[Keys.THEME_MODE])
    }

    override val searchProvider: Flow<SearchProvider> = context.themeDataStore.data.map { prefs ->
        SearchProvider.fromStored(prefs[Keys.SEARCH_PROVIDER])
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
}

class InMemoryThemePreferencesStore : ThemePreferencesStore {
    private val themeState = kotlinx.coroutines.flow.MutableStateFlow(ThemeMode.AUTO)
    private val searchState = kotlinx.coroutines.flow.MutableStateFlow(SearchProvider.GOOGLE_AI)

    override val themeMode: Flow<ThemeMode> = themeState
    override val searchProvider: Flow<SearchProvider> = searchState

    override suspend fun setThemeMode(mode: ThemeMode) {
        themeState.value = mode
    }

    override suspend fun setSearchProvider(provider: SearchProvider) {
        searchState.value = provider
    }
}
