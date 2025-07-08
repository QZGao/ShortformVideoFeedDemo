package com.example.shortformvideofeed.domain.usecase

import com.example.shortformvideofeed.domain.repository.FeedRepository
import com.example.shortformvideofeed.domain.repository.FeedResult
import kotlinx.coroutines.flow.Flow

class ObserveFeedUseCase(
    private val repository: FeedRepository
) {
    operator fun invoke(forceRefresh: Boolean = false): Flow<FeedResult> {
        return repository.observeFeed(forceRefresh)
    }
}
