package com.example.shortformvideofeed.data.remote

import com.example.shortformvideofeed.core.network.NetworkSimulationState
import com.example.shortformvideofeed.domain.model.VideoItem
import java.io.IOException

class SimulatedNetworkFeedRemoteDataSource(
    private val delegate: FeedRemoteDataSource,
    private val simulationState: NetworkSimulationState
) : FeedRemoteDataSource {

    override suspend fun fetchFeed(): List<VideoItem> {
        if (simulationState.isBadNetworkEnabled()) {
            throw IOException("Simulated bad network mode is enabled.")
        }
        return delegate.fetchFeed()
    }
}
