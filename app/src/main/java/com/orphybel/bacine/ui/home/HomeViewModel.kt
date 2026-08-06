package com.orphybel.bacine.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orphybel.bacine.data.repo.MoviePage
import com.orphybel.bacine.data.repo.MovieRepository
import com.orphybel.bacine.domain.Movie
import com.orphybel.bacine.ui.UiState
import com.orphybel.bacine.ui.toUiError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The three shelves on the home screen. */
enum class HomeRow { NOW_PLAYING, VOD, POPULAR }

data class HomeContent(
    val nowPlaying: List<Movie> = emptyList(),
    val vod: List<Movie> = emptyList(),
    val popular: List<Movie> = emptyList(),
) {
    /** The banner film: whatever is currently top of the box office. */
    val featured: Movie? get() = nowPlaying.firstOrNull { it.backdropPath != null }
        ?: nowPlaying.firstOrNull()

    val isEmpty: Boolean get() = nowPlaying.isEmpty() && vod.isEmpty() && popular.isEmpty()
}

class HomeViewModel(private val repository: MovieRepository) : ViewModel() {

    private val _state = MutableStateFlow<UiState<HomeContent>>(UiState.Loading)
    val state: StateFlow<UiState<HomeContent>> = _state.asStateFlow()

    private val paging = HomeRow.entries.associateWith { PageCursor() }
    private val loadingRows = mutableSetOf<HomeRow>()

    init {
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        paging.values.forEach { it.reset() }
        viewModelScope.launch {
            try {
                // The three shelves are independent; fetching them in parallel
                // is the difference between one round trip and three.
                val (nowPlaying, vod, popular) = coroutineScope {
                    listOf(
                        async { repository.nowPlaying(1) },
                        async { repository.recentVod(1) },
                        async { repository.popular(1) },
                    ).awaitAll()
                }
                paging.getValue(HomeRow.NOW_PLAYING).accept(nowPlaying)
                paging.getValue(HomeRow.VOD).accept(vod)
                paging.getValue(HomeRow.POPULAR).accept(popular)

                _state.value = UiState.Success(
                    HomeContent(
                        nowPlaying = nowPlaying.items,
                        vod = vod.items,
                        popular = popular.items,
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _state.value = UiState.Failure(e.toUiError())
            }
        }
    }

    fun loadMore(row: HomeRow) {
        val cursor = paging.getValue(row)
        if (!cursor.hasMore || row in loadingRows) return
        loadingRows += row

        viewModelScope.launch {
            try {
                val next = cursor.page + 1
                val page = when (row) {
                    HomeRow.NOW_PLAYING -> repository.nowPlaying(next)
                    HomeRow.VOD -> repository.recentVod(next)
                    HomeRow.POPULAR -> repository.popular(next)
                }
                cursor.accept(page)
                _state.update { current ->
                    val content = (current as? UiState.Success)?.data ?: return@update current
                    UiState.Success(content.append(row, page.items))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                // A failed extra page is not worth replacing the screen the
                // user is already reading; the row simply stops growing.
                cursor.stop()
            } finally {
                loadingRows -= row
            }
        }
    }

    private fun HomeContent.append(row: HomeRow, items: List<Movie>): HomeContent = when (row) {
        HomeRow.NOW_PLAYING -> copy(nowPlaying = (nowPlaying + items).distinctBy { it.id })
        HomeRow.VOD -> copy(vod = (vod + items).distinctBy { it.id })
        HomeRow.POPULAR -> copy(popular = (popular + items).distinctBy { it.id })
    }
}

/** Tracks how far into a paginated endpoint a row has read. */
private class PageCursor {
    var page: Int = 0
        private set
    var hasMore: Boolean = true
        private set

    fun accept(result: MoviePage) {
        page = result.page
        hasMore = result.hasMore
    }

    fun stop() {
        hasMore = false
    }

    fun reset() {
        page = 0
        hasMore = true
    }
}
