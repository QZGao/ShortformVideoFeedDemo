package com.example.shortformvideofeed.app

import android.app.Application
import androidx.room.Room
import com.example.shortformvideofeed.data.local.AppDatabase
import com.example.shortformvideofeed.data.local.FeedLocalJsonDataSource
import com.example.shortformvideofeed.data.local.SharedPreferencesVideoInteractionStore
import com.example.shortformvideofeed.data.remote.SimulatedNetworkFeedRemoteDataSource
import com.example.shortformvideofeed.data.remote.DefaultFeedRemoteDataSource
import com.example.shortformvideofeed.data.repository.FeedRepositoryImpl
import com.example.shortformvideofeed.core.network.NetworkSimulationState
import com.example.shortformvideofeed.domain.repository.FeedRepository
import com.example.shortformvideofeed.data.local.VideoInteractionStore

class AppContainer(application: Application) {

    val networkSimulationState: NetworkSimulationState by lazy {
        NetworkSimulationState()
    }

    val videoInteractionStore: VideoInteractionStore by lazy {
        SharedPreferencesVideoInteractionStore(application)
    }

    val feedRepository: FeedRepository by lazy {
        val db = Room.databaseBuilder(
            application,
            AppDatabase::class.java,
            "shortform_feed_db"
        ).build()

        FeedRepositoryImpl(
            localDataSource = FeedLocalJsonDataSource(application),
            remoteDataSource = SimulatedNetworkFeedRemoteDataSource(
                delegate = DefaultFeedRemoteDataSource(),
                simulationState = networkSimulationState
            ),
            dao = db.videoDao()
        )
    }
}
