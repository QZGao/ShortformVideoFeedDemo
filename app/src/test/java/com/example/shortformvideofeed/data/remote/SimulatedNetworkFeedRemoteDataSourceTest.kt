package com.example.shortformvideofeed.data.remote

import com.example.shortformvideofeed.core.network.NetworkSimulationState
import com.example.shortformvideofeed.domain.model.VideoItem
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SimulatedNetworkFeedRemoteDataSourceTest {

    @Test
    fun whenNetworkSimulationEnabled_throwsDuringFetch() = runTest {
        val remoteDataSource = FakeFeedRemoteDataSource(
            listOf(
                VideoItem(
                    id = "1",
                    title = "title",
                    videoUrl = "https://example.com/video.mp4",
                    thumbnailUrl = "https://example.com/video.jpg",
                    durationMs = 1000L,
                    authorName = "author",
                    description = "desc",
                    orderIndex = 1
                )
            )
        )
        val simulationState = NetworkSimulationState()
        val source = SimulatedNetworkFeedRemoteDataSource(remoteDataSource, simulationState)

        simulationState.setBadNetworkEnabled(false)
        assertEquals(1, source.fetchFeed().size)

        simulationState.setBadNetworkEnabled(true)
        assertFailsWith<Exception> {
            source.fetchFeed()
        }
    }

    private class FakeFeedRemoteDataSource(private val items: List<VideoItem>) : FeedRemoteDataSource {
        override suspend fun fetchFeed(): List<VideoItem> = items
    }
}
