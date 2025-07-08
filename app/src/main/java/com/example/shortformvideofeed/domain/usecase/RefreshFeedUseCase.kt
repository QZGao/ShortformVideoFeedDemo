package com.example.shortformvideofeed.domain.usecase

import com.example.shortformvideofeed.domain.repository.FeedRepository
import com.example.shortformvideofeed.domain.repository.FeedResult
import kotlinx.coroutines.flow.Flow

class RefreshFeedUseCase(
    private val repository: FeedRepository
) {
    operator fun invoke(): Flow<FeedResult> {
        return repository.observeFeed(forceRefresh = true)
    }
}
