package com.orphybel.bacine.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orphybel.bacine.data.prefs.SettingsStore
import com.orphybel.bacine.data.remote.TmdbClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsState(
    val storedKey: String = "",
    val hasBuildTimeKey: Boolean = false,
    val region: String = "FR",
    val checking: Boolean = false,
    /** The last key the user submitted was rejected by TMDB. */
    val invalid: Boolean = false,
    /** Set once after a successful save, so the caller can navigate onward. */
    val savedAt: Long? = null,
    val networkError: String? = null,
) {
    val hasAnyKey: Boolean get() = storedKey.isNotBlank() || hasBuildTimeKey

    /** Never show the whole key back on a screen anyone in the room can read. */
    val maskedStoredKey: String
        get() = if (storedKey.length <= 8) "••••" else "${storedKey.take(4)}…${storedKey.takeLast(4)}"
}

class SettingsViewModel(
    private val settings: SettingsStore,
    private val client: TmdbClient,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState(hasBuildTimeKey = settings.hasBuildTimeKey))
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(settings.storedApiKey, settings.locale) { key, locale -> key to locale }
                .collect { (key, locale) ->
                    _state.update { it.copy(storedKey = key, region = locale.region) }
                }
        }
    }

    /**
     * Validates a key against TMDB before storing it.
     *
     * Checking here rather than on the next screen means a typo is caught while
     * the user still has the keyboard open, instead of surfacing later as a
     * generic "something went wrong".
     */
    fun save(candidate: String) {
        val trimmed = candidate.trim()
        if (trimmed.isBlank()) {
            _state.update { it.copy(invalid = true) }
            return
        }

        _state.update { it.copy(checking = true, invalid = false, networkError = null) }
        viewModelScope.launch {
            try {
                if (client.validateKey(trimmed)) {
                    settings.setApiKey(trimmed)
                    _state.update {
                        it.copy(checking = false, invalid = false, savedAt = System.currentTimeMillis())
                    }
                } else {
                    _state.update { it.copy(checking = false, invalid = true) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // A network failure is not the same as a bad key; saying so
                // stops the user from retyping a perfectly good one.
                _state.update {
                    it.copy(checking = false, networkError = e.message ?: "Erreur réseau.")
                }
            }
        }
    }

    fun clearKey() {
        viewModelScope.launch { settings.clearApiKey() }
    }

    fun consumeSaved() {
        _state.update { it.copy(savedAt = null) }
    }
}
