package com.example.shortformvideofeed.domain.usecase

import androidx.paging.PagingData
import com.example.shortformvideofeed.domain.model.VideoItem
import com.example.shortformvideofeed.domain.repository.FeedRepository
import kotlinx.coroutines.flow.Flow

class ObservePagedFeedUseCase(
    private val repository: FeedRepository
) {
    operator fun invoke(forceRefresh: Boolean = false): Flow<PagingData<VideoItem>> {
        return repository.observePagedFeed(forceRefresh)
    }
}
