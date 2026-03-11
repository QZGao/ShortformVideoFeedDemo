package com.example.shortformvideofeed.ui.feed

import com.example.shortformvideofeed.domain.model.VideoItem
import com.example.shortformvideofeed.domain.model.PreloadMode
import com.example.shortformvideofeed.domain.repository.FeedSource

data class FeedUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val items: List<VideoItem> = emptyList(),
    val activeItemIndex: Int = 0,
    val source: FeedSource = FeedSource.UNKNOWN,
    val errorMessage: String? = null,
    val preloadMode: PreloadMode = PreloadMode.NEXT_1,
    val likedItemIds: Set<String> = emptySet(),
    val isBadNetworkEnabled: Boolean = false
)
