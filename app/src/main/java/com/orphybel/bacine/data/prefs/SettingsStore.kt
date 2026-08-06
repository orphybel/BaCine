package com.orphybel.bacine.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.orphybel.bacine.BuildConfig
import com.orphybel.bacine.data.remote.TmdbLocale
import com.orphybel.bacine.domain.CatalogQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bacine_settings")

/**
 * Persisted user settings.
 *
 * The TMDB key can come from two places: a key baked in at build time by CI, or
 * one the user typed on the device. The stored key wins so the user can always
 * override a stale build-time key without reinstalling.
 */
class SettingsStore(context: Context) {

    private val store = context.applicationContext.dataStore

    private val json = Json { ignoreUnknownKeys = true }

    private val prefs: Flow<Preferences> = store.data.catch { cause ->
        // A corrupt preferences file must not take the whole app down.
        if (cause is IOException) emit(emptyPreferences()) else throw cause
    }

    /** The key the user typed, if any. Empty when unset. */
    val storedApiKey: Flow<String> = prefs.map { it[KEY_API].orEmpty() }

    /** Stored key first, build-time key as the fallback. Empty means "none". */
    val effectiveApiKey: Flow<String> = prefs.map { it.resolveKey() }

    val locale: Flow<TmdbLocale> = prefs.map {
        TmdbLocale(
            language = it[KEY_LANGUAGE] ?: TmdbLocale.FRANCE.language,
            region = it[KEY_REGION] ?: TmdbLocale.FRANCE.region,
        )
    }

    val catalogQuery: Flow<CatalogQuery> = prefs.map { preferences ->
        preferences[KEY_CATALOG_QUERY]
            ?.let { runCatching { json.decodeFromString<CatalogQuery>(it) }.getOrNull() }
            ?: CatalogQuery.DEFAULT
    }

    /** One-shot read, for callers that cannot collect a flow (the HTTP client). */
    suspend fun currentApiKey(): String = prefs.first().resolveKey()

    suspend fun setApiKey(value: String) {
        store.edit { it[KEY_API] = value.trim() }
    }

    suspend fun clearApiKey() {
        store.edit { it.remove(KEY_API) }
    }

    suspend fun setRegion(region: String) {
        store.edit { it[KEY_REGION] = region.uppercase() }
    }

    suspend fun setCatalogQuery(query: CatalogQuery) {
        store.edit { it[KEY_CATALOG_QUERY] = json.encodeToString(query) }
    }

    private fun Preferences.resolveKey(): String =
        this[KEY_API]?.takeIf { it.isNotBlank() } ?: BuildConfig.TMDB_API_KEY

    /** True when CI baked a key in, so settings can say where the key came from. */
    val hasBuildTimeKey: Boolean get() = BuildConfig.TMDB_API_KEY.isNotBlank()

    private companion object {
        val KEY_API = stringPreferencesKey("tmdb_api_key")
        val KEY_REGION = stringPreferencesKey("region")
        val KEY_LANGUAGE = stringPreferencesKey("language")
        val KEY_CATALOG_QUERY = stringPreferencesKey("catalog_query")
    }
}
