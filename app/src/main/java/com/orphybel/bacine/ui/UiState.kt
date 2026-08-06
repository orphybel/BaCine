package com.orphybel.bacine.ui

import com.orphybel.bacine.data.remote.TmdbException

/** What went wrong, at the granularity the UI actually reacts to. */
enum class ErrorKind {
    /** No key anywhere — the app should send the user to onboarding. */
    MISSING_KEY,

    /** TMDB rejected the key; settings is the place to fix it. */
    INVALID_KEY,

    /** Worth offering a retry button. */
    NETWORK,

    OTHER,
}

data class UiError(val kind: ErrorKind, val message: String)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Failure(val error: UiError) : UiState<Nothing>
}

/** Turns a thrown [TmdbException] into something a screen can render. */
fun Throwable.toUiError(): UiError = when (this) {
    is TmdbException.MissingKey -> UiError(ErrorKind.MISSING_KEY, message.orEmpty())
    is TmdbException.InvalidKey -> UiError(ErrorKind.INVALID_KEY, message.orEmpty())
    is TmdbException.Network -> UiError(ErrorKind.NETWORK, message.orEmpty())
    is TmdbException.Http -> UiError(ErrorKind.OTHER, message.orEmpty())
    is TmdbException.Parse -> UiError(ErrorKind.OTHER, message.orEmpty())
    else -> UiError(ErrorKind.OTHER, message ?: "Erreur inattendue.")
}
