package com.orphybel.bacine.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orphybel.bacine.data.repo.MovieRepository
import com.orphybel.bacine.domain.Movie
import com.orphybel.bacine.ui.UiError
import com.orphybel.bacine.ui.toUiError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchState(
    val term: String = "",
    val results: List<Movie> = emptyList(),
    val searching: Boolean = false,
    val error: UiError? = null,
) {
    val showEmptyPrompt: Boolean get() = term.isBlank()
    val showNoResults: Boolean
        get() = term.isNotBlank() && !searching && error == null && results.isEmpty()
}

@OptIn(FlowPreview::class)
class SearchViewModel(private val repository: MovieRepository) : ViewModel() {

    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private val terms = MutableStateFlow("")

    init {
        viewModelScope.launch {
            terms
                // Typing on a remote is slow and every keystroke would
                // otherwise be a request.
                .debounce(DEBOUNCE_MS)
                .distinctUntilChanged()
                // collectLatest cancels an in-flight search when the term
                // changes again, so results can never arrive out of order.
                .collectLatest { term -> runSearch(term) }
        }
    }

    private suspend fun runSearch(term: String) {
        if (term.isBlank()) {
            _state.update { it.copy(results = emptyList(), searching = false, error = null) }
            return
        }
        _state.update { it.copy(searching = true, error = null) }
        try {
            val page = repository.search(term, page = 1)
            _state.update { it.copy(results = page.items, searching = false) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            _state.update {
                it.copy(searching = false, results = emptyList(), error = e.toUiError())
            }
        }
    }

    fun onTermChange(term: String) {
        _state.update { it.copy(term = term) }
        terms.value = term
    }

    fun retry() {
        val term = _state.value.term
        _state.update { it.copy(error = null) }
        // distinctUntilChanged would swallow an identical term, so break the
        // value first to force the search to run again.
        terms.value = ""
        terms.value = term
    }

    private companion object {
        const val DEBOUNCE_MS = 400L
    }
}
