package com.orphybel.bacine.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orphybel.bacine.data.prefs.SettingsStore
import com.orphybel.bacine.data.repo.MovieRepository
import com.orphybel.bacine.domain.CatalogQuery
import com.orphybel.bacine.domain.Genre
import com.orphybel.bacine.domain.Movie
import com.orphybel.bacine.ui.UiState
import com.orphybel.bacine.ui.toUiError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CatalogContent(
    val movies: List<Movie> = emptyList(),
    val genres: List<Genre> = emptyList(),
    val query: CatalogQuery = CatalogQuery.DEFAULT,
    val loadingMore: Boolean = false,
)

class CatalogViewModel(
    private val repository: MovieRepository,
    private val settings: SettingsStore,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<CatalogContent>>(UiState.Loading)
    val state: StateFlow<UiState<CatalogContent>> = _state.asStateFlow()

    private var query: CatalogQuery = CatalogQuery.DEFAULT
    private var genres: List<Genre> = emptyList()
    private var page = 0
    private var hasMore = true
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            // The last filters the user chose are restored on entry; nobody
            // wants to re-pick a genre every time they open the catalog.
            query = settings.catalogQuery.first()
            reload()
        }
    }

    /** Applies new filters, persists them, and restarts from page one. */
    fun updateQuery(newQuery: CatalogQuery) {
        if (newQuery == query) return
        query = newQuery
        viewModelScope.launch { settings.setCatalogQuery(newQuery) }
        reload()
    }

    fun resetFilters() = updateQuery(CatalogQuery.DEFAULT)

    fun reload() {
        loadJob?.cancel()
        page = 0
        hasMore = true
        _state.value = UiState.Loading

        loadJob = viewModelScope.launch {
            try {
                if (genres.isEmpty()) {
                    // A failed genre list must not block the catalog itself.
                    genres = runCatching { repository.genres() }.getOrDefault(emptyList())
                }
                val result = repository.discover(query, page = 1)
                page = result.page
                hasMore = result.hasMore
                _state.value = UiState.Success(
                    CatalogContent(movies = result.items, genres = genres, query = query)
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _state.value = UiState.Failure(e.toUiError())
            }
        }
    }

    fun loadMore() {
        val current = _state.value as? UiState.Success ?: return
        if (!hasMore || current.data.loadingMore) return

        _state.update { (it as? UiState.Success)?.let { s -> UiState.Success(s.data.copy(loadingMore = true)) } ?: it }

        viewModelScope.launch {
            try {
                val result = repository.discover(query, page + 1)
                page = result.page
                hasMore = result.hasMore
                _state.update { existing ->
                    val content = (existing as? UiState.Success)?.data ?: return@update existing
                    UiState.Success(
                        content.copy(
                            movies = (content.movies + result.items).distinctBy { it.id },
                            loadingMore = false,
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                hasMore = false
                _state.update { existing ->
                    val content = (existing as? UiState.Success)?.data ?: return@update existing
                    UiState.Success(content.copy(loadingMore = false))
                }
            }
        }
    }
}
