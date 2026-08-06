package com.orphybel.bacine.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orphybel.bacine.data.repo.MovieRepository
import com.orphybel.bacine.domain.MovieDetails
import com.orphybel.bacine.ui.UiState
import com.orphybel.bacine.ui.toUiError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DetailViewModel(
    private val repository: MovieRepository,
    private val movieId: Int,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<MovieDetails>>(UiState.Loading)
    val state: StateFlow<UiState<MovieDetails>> = _state.asStateFlow()

    init {
        load()
    }

    fun load(forceRefresh: Boolean = false) {
        _state.value = UiState.Loading
        viewModelScope.launch {
            try {
                _state.value = UiState.Success(repository.details(movieId, forceRefresh))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _state.value = UiState.Failure(e.toUiError())
            }
        }
    }
}
