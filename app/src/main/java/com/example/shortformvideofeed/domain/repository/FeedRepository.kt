package com.example.shortformvideofeed.domain.repository

import com.example.shortformvideofeed.domain.model.VideoItem
import kotlinx.coroutines.flow.Flow

enum class FeedSource {
    UNKNOWN,
    CACHE,
    LOCAL_ASSET,
    REMOTE
}

sealed interface FeedResult {
    data object Loading : FeedResult

    data class Success(
        val items: List<VideoItem>,
        val source: FeedSource
    ) : FeedResult

    data class Error(
        val message: String
    ) : FeedResult
}

interface FeedRepository {
    fun observeFeed(forceRefresh: Boolean = false): Flow<FeedResult>
}
