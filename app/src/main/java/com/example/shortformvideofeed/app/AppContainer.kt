package com.example.shortformvideofeed.app

import android.app.Application
import androidx.room.Room
import com.example.shortformvideofeed.data.local.AppDatabase
import com.example.shortformvideofeed.data.local.FeedLocalJsonDataSource
import com.example.shortformvideofeed.data.remote.DefaultFeedRemoteDataSource
import com.example.shortformvideofeed.data.repository.FeedRepositoryImpl
import com.example.shortformvideofeed.domain.repository.FeedRepository

class AppContainer(application: Application) {

    val feedRepository: FeedRepository by lazy {
        val db = Room.databaseBuilder(
            application,
            AppDatabase::class.java,
            "shortform_feed_db"
        ).build()

        FeedRepositoryImpl(
            localDataSource = FeedLocalJsonDataSource(application),
            remoteDataSource = DefaultFeedRemoteDataSource(),
            dao = db.videoDao()
        )
    }
}
