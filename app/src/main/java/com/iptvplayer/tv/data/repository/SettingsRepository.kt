package com.iptvplayer.tv.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val TMDB_API_KEY = stringPreferencesKey("tmdb_api_key")
    }

    /**
     * Get TMDB API key
     */
    fun getTmdbApiKey(): Flow<String> {
        return context.dataStore.data.map { prefs ->
            prefs[TMDB_API_KEY] ?: ""
        }
    }

    /**
     * Get TMDB API key synchronously (for service initialization)
     */
    suspend fun getTmdbApiKeyOnce(): String {
        return context.dataStore.data.first()[TMDB_API_KEY] ?: ""
    }

    /**
     * Set TMDB API key
     */
    suspend fun setTmdbApiKey(apiKey: String) {
        context.dataStore.edit { prefs ->
            prefs[TMDB_API_KEY] = apiKey.trim()
        }
    }
    /**
     * Get hidden groups for a playlist.
     * Returns set of group names that should be hidden.
     */
    fun getHiddenGroups(playlistId: String): Flow<Set<String>> {
        val key = stringSetPreferencesKey("hidden_groups_$playlistId")
        return context.dataStore.data.map { prefs ->
            prefs[key] ?: emptySet()
        }
    }

    /**
     * Set hidden groups for a playlist.
     */
    suspend fun setHiddenGroups(playlistId: String, hiddenGroups: Set<String>) {
        val key = stringSetPreferencesKey("hidden_groups_$playlistId")
        context.dataStore.edit { prefs ->
            prefs[key] = hiddenGroups
        }
    }

    /**
     * Toggle a single group's visibility.
     */
    suspend fun toggleGroupVisibility(playlistId: String, group: String, allGroups: List<String>) {
        val key = stringSetPreferencesKey("hidden_groups_$playlistId")
        context.dataStore.edit { prefs ->
            val current = prefs[key]?.toMutableSet() ?: mutableSetOf()
            if (current.contains(group)) {
                current.remove(group)
            } else {
                current.add(group)
            }
            prefs[key] = current
        }
    }

    /**
     * Hide all groups (select none).
     */
    suspend fun hideAllGroups(playlistId: String, allGroups: List<String>) {
        val key = stringSetPreferencesKey("hidden_groups_$playlistId")
        context.dataStore.edit { prefs ->
            prefs[key] = allGroups.toSet()
        }
    }

    /**
     * Show all groups (select all).
     */
    suspend fun showAllGroups(playlistId: String) {
        val key = stringSetPreferencesKey("hidden_groups_$playlistId")
        context.dataStore.edit { prefs ->
            prefs[key] = emptySet()
        }
    }
}
