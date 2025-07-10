package com.example.shortformvideofeed.data.repository

import androidx.paging.PagingSource
import com.example.shortformvideofeed.data.local.FeedLocalDataSource
import com.example.shortformvideofeed.data.local.VideoDao
import com.example.shortformvideofeed.data.local.VideoEntity
import com.example.shortformvideofeed.data.remote.FeedRemoteDataSource
import com.example.shortformvideofeed.domain.model.VideoItem
import com.example.shortformvideofeed.domain.repository.FeedResult
import com.example.shortformvideofeed.domain.repository.FeedSource
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FeedRepositoryImplTest {

    @Test
    fun observeFeed_whenCacheExistsAndRemoteSucceeds_emitsCacheThenRemote() = runTest {
        val dao = FakeVideoDao(
            listOf(
                videoEntity("cache-1", 0)
            )
        )
        val repository = FeedRepositoryImpl(
            localDataSource = FakeLocalDataSource(emptyList()),
            remoteDataSource = FakeRemoteDataSource(listOf(video("remote-1", 10))),
            dao = dao
        )

        val results = repository.observeFeed().toList()

        assertIs<FeedResult.Loading>(results[0])
        assertIs<FeedResult.Success>(results[1])
        assertEquals(FeedSource.CACHE, (results[1] as FeedResult.Success).source)
        assertIs<FeedResult.Success>(results[2])
        assertEquals(FeedSource.REMOTE, (results[2] as FeedResult.Success).source)
        assertEquals("remote-1", (results[2] as FeedResult.Success).items.first().id)
    }

    @Test
    fun observeFeed_whenRemoteFailsWithCachedItems_emitsRecoverableError() = runTest {
        val dao = FakeVideoDao(
            listOf(
                videoEntity("cache-1", 0)
            )
        )
        val repository = FeedRepositoryImpl(
            localDataSource = FakeLocalDataSource(emptyList()),
            remoteDataSource = FakeRemoteDataSource(fail = true),
            dao = dao
        )

        val results = repository.observeFeed().toList()

        assertIs<FeedResult.Loading>(results[0])
        assertIs<FeedResult.Success>(results[1])
        assertEquals(FeedSource.CACHE, (results[1] as FeedResult.Success).source)
        assertIs<FeedResult.Error>(results[2])
        val error = results[2] as FeedResult.Error
        assertEquals(FeedSource.REMOTE, error.source)
        assertTrue(error.recoverable)
    }

    @Test
    fun observeFeed_whenNoCacheOrSeedAndRemoteFails_emitsFatalError() = runTest {
        val repository = FeedRepositoryImpl(
            localDataSource = FakeLocalDataSource(emptyList()),
            remoteDataSource = FakeRemoteDataSource(fail = true),
            dao = FakeVideoDao()
        )

        val results = repository.observeFeed().toList()

        assertIs<FeedResult.Loading>(results[0])
        assertIs<FeedResult.Error>(results[1])
        val error = results[1] as FeedResult.Error
        assertEquals(FeedSource.REMOTE, error.source)
        assertTrue(!error.recoverable)
    }

    private fun video(id: String, index: Int) = VideoItem(
        id = id,
        title = "Title $id",
        videoUrl = "https://example.com/$id.mp4",
        thumbnailUrl = "https://example.com/$id.jpg",
        durationMs = 1000L,
        authorName = "Author",
        description = "",
        orderIndex = index
    )

    private fun videoEntity(id: String, index: Int) = VideoEntity(
        id = id,
        title = "Title $id",
        videoUrl = "https://example.com/$id.mp4",
        thumbnailUrl = "https://example.com/$id.jpg",
        durationMs = 1000L,
        authorName = "Author",
        description = "",
        orderIndex = index,
        cachedAt = 0L
    )

    private class FakeLocalDataSource(
        private val seed: List<VideoItem>
    ) : FeedLocalDataSource {
        override suspend fun loadSeedFeed(): List<VideoItem> = seed
    }

    private class FakeRemoteDataSource(
        private val payload: List<VideoItem> = emptyList(),
        private val fail: Boolean = false,
        private val error: Throwable = IOException("mock failure")
    ) : FeedRemoteDataSource {
        override suspend fun fetchFeed(): List<VideoItem> {
            if (fail) throw error
            return payload
        }
    }

    private class FakeVideoDao(initialItems: List<VideoEntity> = emptyList()) : VideoDao {
        private val items = initialItems.toMutableList()

        override suspend fun count(): Int = items.size

        override suspend fun getAllOrdered(): List<VideoEntity> = items.sortedBy { it.orderIndex }

        override fun pagingSource() = throw UnsupportedOperationException("Not needed for repository list tests")

        override suspend fun upsertAll(newItems: List<VideoEntity>) {
            items.clear()
            items.addAll(newItems)
        }

        override suspend fun clear() {
            items.clear()
        }
    }
}
