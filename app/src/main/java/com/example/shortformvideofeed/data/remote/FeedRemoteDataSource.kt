package com.example.shortformvideofeed.data.remote

import com.example.shortformvideofeed.BuildConfig
import com.example.shortformvideofeed.data.mapper.toDomain
import com.example.shortformvideofeed.domain.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

interface FeedRemoteDataSource {
    suspend fun fetchFeed(): List<VideoItem>
}

class DefaultFeedRemoteDataSource : FeedRemoteDataSource {

    private val api = Retrofit.Builder()
        .baseUrl("https://raw.githubusercontent.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(FeedApi::class.java)

    override suspend fun fetchFeed(): List<VideoItem> = withContext(Dispatchers.IO) {
        api.fetchFeed(BuildConfig.FEED_REMOTE_URL).mapIndexed { index, dto -> dto.toDomain(index) }
    }
}
