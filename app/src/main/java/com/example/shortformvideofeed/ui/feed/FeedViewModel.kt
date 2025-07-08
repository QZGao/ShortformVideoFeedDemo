package com.example.shortformvideofeed.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shortformvideofeed.domain.repository.FeedResult
import com.example.shortformvideofeed.domain.repository.FeedSource
import com.example.shortformvideofeed.domain.usecase.ObserveFeedUseCase
import com.example.shortformvideofeed.domain.usecase.RefreshFeedUseCase
import com.example.shortformvideofeed.player.FeedPlayerManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FeedViewModel(
    private val observeFeedUseCase: ObserveFeedUseCase,
    private val refreshFeedUseCase: RefreshFeedUseCase,
    private val playerManager: FeedPlayerManager
) : ViewModel() {

    private val _state = MutableStateFlow(FeedUiState())
    val state = _state.asStateFlow()

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
                    }
                    is FeedResult.Error -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                errorMessage = result.message,
                                source = FeedSource.UNKNOWN
                            )
                        }
                    }
                }
            }
        }
    }

    fun onPullToRefresh() {
        _state.update { it.copy(isRefreshing = true) }
        load(forceRefresh = true)
    }

    fun onActiveItemChanged(index: Int) {
        val items = _state.value.items
        val safeIndex = index.coerceIn(0, (items.size - 1).coerceAtLeast(0))
        _state.update { it.copy(activeItemIndex = safeIndex) }

        val item = items.getOrNull(safeIndex)
        playerManager.activate(item?.id, item?.videoUrl)
        playerManager.preload(items.getOrNull(safeIndex + 1)?.videoUrl)
    }
}
