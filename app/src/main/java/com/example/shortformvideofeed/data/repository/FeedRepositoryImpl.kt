package com.example.shortformvideofeed.data.repository

import com.example.shortformvideofeed.data.local.FeedLocalDataSource
import com.example.shortformvideofeed.data.local.VideoDao
import com.example.shortformvideofeed.data.mapper.toDomain
import com.example.shortformvideofeed.data.mapper.toEntity
import com.example.shortformvideofeed.data.remote.FeedRemoteDataSource
import com.example.shortformvideofeed.domain.model.VideoItem
import com.example.shortformvideofeed.domain.repository.FeedRepository
import com.example.shortformvideofeed.domain.repository.FeedResult
import com.example.shortformvideofeed.domain.repository.FeedSource
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import retrofit2.HttpException
import java.io.IOException
import java.net.UnknownHostException

class FeedRepositoryImpl(
    private val localDataSource: FeedLocalDataSource,
    private val remoteDataSource: FeedRemoteDataSource,
    private val dao: VideoDao
) : FeedRepository {

    override fun observeFeed(forceRefresh: Boolean): Flow<FeedResult> = flow {
        emit(FeedResult.Loading)

        val cachedItems = readCachedItems()
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

        val hasSeedItems = localItems.isNotEmpty()
        if (shouldAttemptRemoteFetch(forceRefresh, cachedItems.isNotEmpty(), shouldSeedFromAsset)) {
            runCatching { remoteDataSource.fetchFeed() }
                .onSuccess { remoteItems ->
                    when {
                        remoteItems.isNotEmpty() -> {
                            saveToDb(remoteItems)
                            emit(FeedResult.Success(remoteItems, FeedSource.REMOTE))
                        }
                        hasSeedItems || cachedItems.isNotEmpty() -> {
                            emit(
                                FeedResult.Error(
                                    message = "Remote feed was empty.",
                                    source = FeedSource.REMOTE,
                                    recoverable = true
                                )
                            )
                        }
                        else -> {
                            emit(
                                FeedResult.Error(
                                    message = "Remote feed was empty.",
                                    source = FeedSource.REMOTE,
                                    recoverable = false
                                )
                            )
                        }
                    }
                }
                .onFailure { error ->
                    val message = remoteErrorMessage(error)
                    if (cachedItems.isEmpty() && hasSeedItems.not()) {
                        emit(
                            FeedResult.Error(
                                message = message,
                                source = FeedSource.REMOTE,
                                recoverable = false
                            )
                        )
                    } else {
                        emit(
                            FeedResult.Error(
                                message = message,
                                source = FeedSource.REMOTE,
                                recoverable = true
                            )
                        )
                    }
                }
        }
    }

    override fun observePagedFeed(forceRefresh: Boolean): Flow<PagingData<VideoItem>> = Pager(
        config = PagingConfig(pageSize = 10)
    ) {
        dao.pagingSource()
    }.flow
        .map { source ->
            source.map { item ->
                item.toDomain()
            }
        }
        .onStart {
            val cachedCount = readCachedCount()
            val shouldSeedFromAsset = cachedCount == 0
            if (shouldSeedFromAsset) {
                val seeded = runCatching { localDataSource.loadSeedFeed() }.getOrElse { emptyList() }
                if (seeded.isNotEmpty()) {
                    saveToDb(seeded)
                }
            }

            if (forceRefresh || cachedCount > 0 || shouldSeedFromAsset) {
                runCatching { remoteDataSource.fetchFeed() }
                    .onSuccess { remoteItems ->
                        if (remoteItems.isNotEmpty()) {
                            saveToDb(remoteItems)
                        }
                    }
                    .onFailure {
                        // Ignore for paging stream; observers get UI errors via observeFeed
                    }
            }
        }

    private suspend fun saveToDb(items: List<VideoItem>) {
        dao.clear()
        dao.upsertAll(items.map { it.toEntity() })
    }

    private suspend fun readCachedItems(): List<VideoItem> = runCatching { dao.getAllOrdered().map { it.toDomain() } }
        .getOrDefault(emptyList())

    private suspend fun readCachedCount(): Int = runCatching { dao.count() }.getOrDefault(0)

    private fun shouldAttemptRemoteFetch(
        forceRefresh: Boolean,
        hasCachedItems: Boolean,
        shouldSeedFromAsset: Boolean
    ): Boolean {
        return forceRefresh || hasCachedItems || shouldSeedFromAsset
    }

    private fun remoteErrorMessage(error: Throwable): String {
        return when (error) {
            is HttpException -> {
                when (error.code()) {
                    404 -> "Remote feed URL not found (404). Update FEED_REMOTE_URL in app/build.gradle.kts."
                    else -> "Remote request failed (${error.code()}): ${error.message()}"
                }
            }
            is IOException, is UnknownHostException -> "Network error while downloading remote feed."
            else -> error.message ?: "Unknown remote fetch error."
        }
    }
}
