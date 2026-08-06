package com.orphybel.bacine.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orphybel.bacine.data.repo.MovieRepository
import com.orphybel.bacine.domain.Video
import com.orphybel.bacine.ui.ErrorKind
import com.orphybel.bacine.ui.UiError
import com.orphybel.bacine.ui.UiState
import com.orphybel.bacine.ui.toUiError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TrailerTarget(val video: Video, val movieTitle: String)

/**
 * Resolves which video to play from a movie id.
 *
 * Going through the repository rather than passing a video key around the
 * navigation graph means the home banner can open a trailer without having
 * loaded the movie's details first — and when they are already cached, from the
 * detail screen, this resolves without touching the network.
 */
class TrailerViewModel(
    private val repository: MovieRepository,
    private val movieId: Int,
    /** When set, play this exact video instead of the best trailer. */
    private val videoKey: String? = null,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<TrailerTarget>>(UiState.Loading)
    val state: StateFlow<UiState<TrailerTarget>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            try {
                val details = repository.details(movieId)
                // An explicitly requested video wins, but fall back to the best
                // trailer if that key is no longer in TMDB's list.
                val trailer = videoKey
                    ?.let { key -> details.videos.firstOrNull { it.key == key } }
                    ?: details.bestTrailer
                _state.value = if (trailer == null) {
                    UiState.Failure(
                        UiError(ErrorKind.OTHER, "Aucune bande-annonce trouvée pour ce film.")
                    )
                } else {
                    UiState.Success(TrailerTarget(trailer, details.movie.title))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _state.value = UiState.Failure(e.toUiError())
            }
        }
    }
}
