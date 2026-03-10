package com.example.shortformvideofeed.domain.usecase

import androidx.paging.PagingData
import com.example.shortformvideofeed.domain.model.VideoItem
import com.example.shortformvideofeed.domain.repository.FeedRepository
import com.example.shortformvideofeed.domain.repository.FeedResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class RefreshFeedUseCaseTest {

    @Test
    fun refreshFeedUseCase_usesForceRefreshFlow() = runTest {
        var observedForceRefresh: Boolean? = null

        val repository = object : FeedRepository {
            override fun observeFeed(forceRefresh: Boolean): Flow<FeedResult> {
                observedForceRefresh = forceRefresh
                return flowOf(FeedResult.Loading)
            }

            override fun observePagedFeed(forceRefresh: Boolean): Flow<PagingData<VideoItem>> {
                return flowOf(PagingData.empty())
            }
        }

        val useCase = RefreshFeedUseCase(repository)

        useCase().collect {
            // collect once
        }

        assertEquals(true, observedForceRefresh)
    }
}
