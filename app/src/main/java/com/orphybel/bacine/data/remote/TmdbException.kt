package com.orphybel.bacine.data.remote

/** Failures the UI needs to tell apart, so it can show the right recovery. */
sealed class TmdbException(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /** No key configured anywhere — the app should route to onboarding. */
    class MissingKey : TmdbException("Aucune clé API TMDB n'est configurée.")

    /** TMDB rejected the key (HTTP 401). The user must fix it in settings. */
    class InvalidKey(message: String) : TmdbException(message)

    /** DNS failure, timeout, airplane mode… retrying may work. */
    class Network(cause: Throwable) : TmdbException("Impossible de joindre TMDB.", cause)

    class Http(val code: Int, message: String) : TmdbException(message)

    class Parse(cause: Throwable) : TmdbException("Réponse TMDB inattendue.", cause)
}
