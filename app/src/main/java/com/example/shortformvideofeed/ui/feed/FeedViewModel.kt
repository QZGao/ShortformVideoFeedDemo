package com.example.shortformvideofeed.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.shortformvideofeed.core.network.NetworkSimulationState
import com.example.shortformvideofeed.domain.model.PreloadMode
import com.example.shortformvideofeed.domain.model.VideoItem
import com.example.shortformvideofeed.domain.repository.FeedResult
import com.example.shortformvideofeed.domain.repository.FeedSource
import com.example.shortformvideofeed.domain.usecase.ObserveFeedUseCase
import com.example.shortformvideofeed.domain.usecase.ObservePagedFeedUseCase
import com.example.shortformvideofeed.domain.usecase.RefreshFeedUseCase
import com.example.shortformvideofeed.player.FeedPlayerController
import com.example.shortformvideofeed.data.local.VideoInteractionStore
import com.example.shortformvideofeed.data.local.InMemoryVideoInteractionStore
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
    private val playerManager: FeedPlayerController,
    private val networkSimulationState: NetworkSimulationState,
    private val videoInteractionStore: VideoInteractionStore = InMemoryVideoInteractionStore()
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
        _state.update {
            it.copy(
                likedItemIds = videoInteractionStore.getLikedItemIds(),
                isBadNetworkEnabled = networkSimulationState.isBadNetworkEnabled()
            )
        }
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
                        val sourceForError = if (shouldKeepItems) _state.value.source else result.source
                        _state.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                errorMessage = result.message,
                                source = sourceForError,
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
        if (items.isEmpty()) return

        val safeIndex = index.coerceIn(0, items.size - 1)
        _state.update { it.copy(activeItemIndex = safeIndex) }

        val currentItem = items.getOrNull(safeIndex)
        playerManager.activate(currentItem?.id, currentItem?.videoUrl, currentItem?.durationMs)
        preloadForActiveIndex(safeIndex, items)
    }

    fun onPreloadModeChanged(mode: PreloadMode) {
        if (_state.value.preloadMode == mode) return
        _state.update { it.copy(preloadMode = mode) }
        val state = _state.value
        preloadForActiveIndex(state.activeItemIndex, state.items)
    }

    fun onLikeToggled(itemId: String) {
        val currentlyLiked = _state.value.likedItemIds.contains(itemId)
        videoInteractionStore.setLiked(itemId, !currentlyLiked)
        val updatedLikedItemIds = videoInteractionStore.getLikedItemIds()
        _state.update { it.copy(likedItemIds = updatedLikedItemIds) }
    }

    fun onBadNetworkModeChanged(enabled: Boolean) {
        networkSimulationState.setBadNetworkEnabled(enabled)
        _state.update { it.copy(isBadNetworkEnabled = enabled) }
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
