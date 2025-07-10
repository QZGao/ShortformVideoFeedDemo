package com.example.shortformvideofeed.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.shortformvideofeed.domain.model.PreloadMode
import com.example.shortformvideofeed.domain.model.VideoItem
import com.example.shortformvideofeed.domain.repository.FeedResult
import com.example.shortformvideofeed.domain.repository.FeedSource
import com.example.shortformvideofeed.domain.usecase.ObserveFeedUseCase
import com.example.shortformvideofeed.domain.usecase.ObservePagedFeedUseCase
import com.example.shortformvideofeed.domain.usecase.RefreshFeedUseCase
import com.example.shortformvideofeed.player.FeedPlayerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

class FeedViewModel(
    private val observeFeedUseCase: ObserveFeedUseCase,
    observePagedFeedUseCase: ObservePagedFeedUseCase,
    private val refreshFeedUseCase: RefreshFeedUseCase,
    private val playerManager: FeedPlayerManager
) : ViewModel() {

    private val _state = MutableStateFlow(FeedUiState())
    val state = _state.asStateFlow()

    private val refreshSignal = MutableStateFlow(0)

    val pagedFeed: StateFlow<PagingData<VideoItem>> = refreshSignal
        .flatMapLatest { forceRefresh ->
            observePagedFeedUseCase(forceRefresh = forceRefresh > 0)
        }
        .cachedIn(viewModelScope)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = PagingData.empty()
        )

    private var loadJob: Job? = null

    init {
        load(forceRefresh = false)
    }

    fun load(forceRefresh: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val flow = if (forceRefresh) refreshFeedUseCase() else observeFeedUseCase(forceRefresh = false)
            flow.collectLatest { result ->
                when (result) {
                    FeedResult.Loading -> _state.update { it.copy(isLoading = true, errorMessage = null) }
                    is FeedResult.Success -> {
                        val previousActiveItemId = _state.value.items.getOrNull(_state.value.activeItemIndex)?.id
                        val safeIndex = result.items.indexOfFirst { it.id == previousActiveItemId }
                            .takeIf { it >= 0 } ?: 0
                        _state.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                items = result.items,
                                source = result.source,
                                errorMessage = null,
                                activeItemIndex = safeIndex
                            )
                        }
                        preloadForActiveIndex(safeIndex, result.items)
                    }
                    is FeedResult.Error -> {
                        val hasExistingItems = _state.value.items.isNotEmpty()
                        val shouldKeepItems = result.recoverable && hasExistingItems
                        _state.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                errorMessage = result.message,
                                source = if (shouldKeepItems) result.source else FeedSource.UNKNOWN,
                                items = if (shouldKeepItems) it.items else emptyList()
                            )
                        }
                    }
                }
            }
        }
    }

    fun onPullToRefresh() {
        _state.update { it.copy(isRefreshing = true) }
        refreshSignal.update { it + 1 }
        load(forceRefresh = true)
    }

    fun onActiveItemChanged(index: Int) {
        val items = _state.value.items
        val safeIndex = index.coerceIn(0, (items.size - 1).coerceAtLeast(0))
        _state.update { it.copy(activeItemIndex = safeIndex) }

        val currentItem = items.getOrNull(safeIndex)
        playerManager.activate(currentItem?.id, currentItem?.videoUrl)
        preloadForActiveIndex(safeIndex, items)
    }

    fun onPreloadModeChanged(mode: PreloadMode) {
        _state.update { it.copy(preloadMode = mode) }
        val state = _state.value
        preloadForActiveIndex(state.activeItemIndex, state.items)
    }

    private fun preloadForActiveIndex(index: Int, items: List<VideoItem>) {
        val preloadItems = when (_state.value.preloadMode) {
            PreloadMode.OFF -> emptyList()
            PreloadMode.NEXT_1 -> listOfNotNull(items.getOrNull(index + 1)?.videoUrl)
            PreloadMode.NEXT_2 -> listOfNotNull(
                items.getOrNull(index + 1)?.videoUrl,
                items.getOrNull(index + 2)?.videoUrl
            )
        }
        playerManager.preload(preloadItems)
    }
}
