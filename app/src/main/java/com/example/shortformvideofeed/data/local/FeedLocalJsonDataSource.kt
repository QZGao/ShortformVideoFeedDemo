package com.example.shortformvideofeed.data.local

import android.content.Context
import com.example.shortformvideofeed.data.remote.FeedDto
import com.example.shortformvideofeed.data.mapper.toDomain
import com.example.shortformvideofeed.domain.model.VideoItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface FeedLocalDataSource {
    suspend fun loadSeedFeed(): List<VideoItem>
}

class FeedLocalJsonDataSource(
    private val context: Context
) : FeedLocalDataSource {

    private val gson by lazy { Gson() }

    override suspend fun loadSeedFeed(): List<VideoItem> = withContext(Dispatchers.IO) {
        val assetStream = context.assets.open("feed.json")
        assetStream.use { input ->
            val json = input.bufferedReader().readText()
            val type = object : TypeToken<List<FeedDto>>() {}.type
            val items = gson.fromJson<List<FeedDto>>(json, type)
            items.mapIndexed { index, dto -> dto.toDomain(index) }
        }
    }
}
