package com.example.shortformvideofeed.data.repository

import com.example.shortformvideofeed.data.local.FeedLocalJsonDataSource
import com.example.shortformvideofeed.data.local.VideoDao
import com.example.shortformvideofeed.data.mapper.toDomain
import com.example.shortformvideofeed.data.mapper.toEntity
import com.example.shortformvideofeed.data.remote.FeedRemoteDataSource
import com.example.shortformvideofeed.domain.model.VideoItem
import com.example.shortformvideofeed.domain.repository.FeedRepository
import com.example.shortformvideofeed.domain.repository.FeedResult
import com.example.shortformvideofeed.domain.repository.FeedSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.IOException

class FeedRepositoryImpl(
    private val localDataSource: FeedLocalJsonDataSource,
    private val remoteDataSource: FeedRemoteDataSource,
    private val dao: VideoDao
) : FeedRepository {

    override fun observeFeed(forceRefresh: Boolean): Flow<FeedResult> = flow {
        emit(FeedResult.Loading)

        val cachedItems = runCatching { dao.getAllOrdered().map { it.toDomain() } }.getOrDefault(emptyList())
        if (cachedItems.isNotEmpty()) {
            emit(FeedResult.Success(cachedItems, FeedSource.CACHE))
        }

        val shouldSeedFromAsset = cachedItems.isEmpty()
        val localItems = if (shouldSeedFromAsset) {
            runCatching { localDataSource.loadSeedFeed() }.getOrElse { emptyList() }
        } else {
            emptyList()
        }
        if (localItems.isNotEmpty()) {
            saveToDb(localItems)
            emit(FeedResult.Success(localItems, FeedSource.LOCAL_ASSET))
        }

        if (forceRefresh || cachedItems.isNotEmpty() || shouldSeedFromAsset) {
            val remoteResult = runCatching { remoteDataSource.fetchFeed() }
            remoteResult.onSuccess { remoteItems ->
                if (remoteItems.isNotEmpty()) {
                    saveToDb(remoteItems)
                    emit(FeedResult.Success(remoteItems, FeedSource.REMOTE))
                } else if (cachedItems.isEmpty() && localItems.isEmpty()) {
                    emit(FeedResult.Error("Remote feed was empty."))
                }
            }.onFailure { error ->
                if (cachedItems.isEmpty() && localItems.isEmpty()) {
                    val message = when (error) {
                        is IOException -> "Network error while downloading remote feed."
                        else -> error.message ?: "Unknown remote fetch error."
                    }
                    emit(FeedResult.Error(message))
                }
            }
        }
    }

    private suspend fun saveToDb(items: List<VideoItem>) {
        dao.clear()
        dao.upsertAll(items.map { it.toEntity() })
    }
}
