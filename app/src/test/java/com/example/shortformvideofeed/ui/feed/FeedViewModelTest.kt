package com.example.shortformvideofeed.ui.feed

import androidx.media3.common.Player
import androidx.paging.PagingData
import com.example.shortformvideofeed.domain.model.PreloadMode
import com.example.shortformvideofeed.domain.model.VideoItem
import com.example.shortformvideofeed.domain.repository.FeedRepository
import com.example.shortformvideofeed.domain.repository.FeedResult
import com.example.shortformvideofeed.domain.repository.FeedSource
import com.example.shortformvideofeed.domain.usecase.ObserveFeedUseCase
import com.example.shortformvideofeed.domain.usecase.ObservePagedFeedUseCase
import com.example.shortformvideofeed.domain.usecase.RefreshFeedUseCase
import com.example.shortformvideofeed.player.FeedPlayerController
import com.example.shortformvideofeed.player.PlaybackUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModelTest {

    @Test
    fun init_loadsFeedAndPreloadsNextItem() = runTest {
        withMainDispatcher(testScheduler) {
            val repository = FakeFeedRepository(
                observeFeedFlow = flowOf(
                    FeedResult.Loading,
                    FeedResult.Success(
                        items = listOf(
                            videoItem("1", "https://example.com/1.mp4"),
                            videoItem("2", "https://example.com/2.mp4"),
                            videoItem("3", "https://example.com/3.mp4")
                        ),
                        source = FeedSource.REMOTE
                    )
                )
            )
            val player = FakeFeedPlayerController()
            val viewModel = FeedViewModel(
                observeFeedUseCase = ObserveFeedUseCase(repository),
                observePagedFeedUseCase = ObservePagedFeedUseCase(repository),
                refreshFeedUseCase = RefreshFeedUseCase(repository),
                playerManager = player
            )

            advanceUntilIdle()

            val state = viewModel.state.value
            assertFalse(state.isLoading)
            assertEquals(FeedSource.REMOTE, state.source)
            assertEquals(3, state.items.size)
            assertEquals(listOf("https://example.com/2.mp4"), player.preloadCalls.last())
        }
    }

    @Test
    fun onActiveItemChanged_updatesActiveIndexAndActivatesPlayer() = runTest {
        withMainDispatcher(testScheduler) {
            val repository = FakeFeedRepository(
                observeFeedFlow = flowOf(
                    FeedResult.Loading,
                    FeedResult.Success(
                        items = listOf(
                            videoItem("1", "https://example.com/1.mp4"),
                            videoItem("2", "https://example.com/2.mp4"),
                            videoItem("3", "https://example.com/3.mp4")
                        ),
                        source = FeedSource.REMOTE
                    )
                )
            )
            val player = FakeFeedPlayerController()
            val viewModel = FeedViewModel(
                observeFeedUseCase = ObserveFeedUseCase(repository),
                observePagedFeedUseCase = ObservePagedFeedUseCase(repository),
                refreshFeedUseCase = RefreshFeedUseCase(repository),
                playerManager = player
            )

            advanceUntilIdle()
            viewModel.onActiveItemChanged(2)
            advanceUntilIdle()

            assertEquals(2, viewModel.state.value.activeItemIndex)
            assertEquals("3" to "https://example.com/3.mp4", player.activateCalls.last())
            assertEquals(emptyList(), player.preloadCalls.last())
        }
    }

    @Test
    fun onPreloadModeChanged_switchesRequestedPreloadWindow() = runTest {
        withMainDispatcher(testScheduler) {
            val repository = FakeFeedRepository(
                observeFeedFlow = flowOf(
                    FeedResult.Loading,
                    FeedResult.Success(
                        items = listOf(
                            videoItem("1", "https://example.com/1.mp4"),
                            videoItem("2", "https://example.com/2.mp4"),
                            videoItem("3", "https://example.com/3.mp4"),
                            videoItem("4", "https://example.com/4.mp4")
                        ),
                        source = FeedSource.REMOTE
                    )
                )
            )
            val player = FakeFeedPlayerController()
            val viewModel = FeedViewModel(
                observeFeedUseCase = ObserveFeedUseCase(repository),
                observePagedFeedUseCase = ObservePagedFeedUseCase(repository),
                refreshFeedUseCase = RefreshFeedUseCase(repository),
                playerManager = player
            )

            advanceUntilIdle()
            viewModel.onPreloadModeChanged(PreloadMode.NEXT_2)
            advanceUntilIdle()

            assertEquals(PreloadMode.NEXT_2, viewModel.state.value.preloadMode)
            assertEquals(listOf("https://example.com/2.mp4", "https://example.com/3.mp4"), player.preloadCalls.last())
        }
    }

    @Test
    fun onPullToRefresh_incrementsRefreshSignalAndRefreshes() = runTest {
        withMainDispatcher(testScheduler) {
            val repository = FakeFeedRepository(
                observeFeedFlow = flowOf(FeedResult.Loading, FeedResult.Success(emptyList(), FeedSource.CACHE)),
                observePagedFlow = flowOf(PagingData.from(emptyList()))
            )
            val player = FakeFeedPlayerController()
            val viewModel = FeedViewModel(
                observeFeedUseCase = ObserveFeedUseCase(repository),
                observePagedFeedUseCase = ObservePagedFeedUseCase(repository),
                refreshFeedUseCase = RefreshFeedUseCase(repository),
                playerManager = player
            )
            advanceUntilIdle()

            assertEquals(listOf(false), repository.observeFeedForceRefreshCalls)
            assertEquals(listOf(false), repository.observePagedForceRefreshCalls)

            viewModel.onPullToRefresh()
            advanceUntilIdle()

            assertTrue(viewModel.state.value.isRefreshing.not())
            assertEquals(listOf(false, true), repository.observeFeedForceRefreshCalls)
            assertEquals(listOf(false, true), repository.observePagedForceRefreshCalls)
        }
    }

    @Test
    fun onPullToRefresh_setsAndClearsRefreshingState() = runTest {
        withMainDispatcher(testScheduler) {
            val repository = FakeFeedRepository(
                observeFeedFlow = flowOf(FeedResult.Loading, FeedResult.Success(emptyList(), FeedSource.REMOTE)),
                observePagedFlow = flowOf(PagingData.from(emptyList()))
            )
            val player = FakeFeedPlayerController()
            val viewModel = FeedViewModel(
                observeFeedUseCase = ObserveFeedUseCase(repository),
                observePagedFeedUseCase = ObservePagedFeedUseCase(repository),
                refreshFeedUseCase = RefreshFeedUseCase(repository),
                playerManager = player
            )
            advanceUntilIdle()

            viewModel.onPullToRefresh()
            assertTrue(viewModel.state.value.isRefreshing)

            advanceUntilIdle()

            assertFalse(viewModel.state.value.isRefreshing)
        }
    }

    private suspend fun withMainDispatcher(
        testScheduler: TestCoroutineScheduler,
        block: suspend () -> Unit
    ) {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            block()
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun videoItem(id: String, videoUrl: String) = VideoItem(
        id = id,
        title = "Title $id",
        videoUrl = videoUrl,
        thumbnailUrl = "https://example.com/$id.jpg",
        durationMs = 12000L,
        authorName = "Author $id",
        description = "Description $id",
        orderIndex = id.toIntOrNull() ?: 0
    )

    private class FakeFeedRepository(
        private val observeFeedFlow: Flow<FeedResult> = flowOf(FeedResult.Loading),
        private val observePagedFlow: Flow<PagingData<VideoItem>> = flowOf(PagingData.empty())
    ) : FeedRepository {
        val observeFeedForceRefreshCalls = mutableListOf<Boolean>()
        val observePagedForceRefreshCalls = mutableListOf<Boolean>()

        override fun observeFeed(forceRefresh: Boolean): Flow<FeedResult> {
            observeFeedForceRefreshCalls.add(forceRefresh)
            return observeFeedFlow
        }

        override fun observePagedFeed(forceRefresh: Boolean): Flow<PagingData<VideoItem>> {
            observePagedForceRefreshCalls.add(forceRefresh)
            return observePagedFlow
        }
    }

    private class FakeFeedPlayerController : FeedPlayerController {
        private val playbackStateFlow = MutableStateFlow(
            PlaybackUiState(selectedItemId = null, playbackState = Player.STATE_IDLE, isBuffering = false)
        )
        override val playbackState = playbackStateFlow
        val activateCalls = mutableListOf<Pair<String?, String?>>()
        val preloadCalls = mutableListOf<List<String>>()

        override fun activate(itemId: String?, videoUrl: String?) {
            activateCalls.add(Pair(itemId, videoUrl))
            playbackStateFlow.update { it.copy(selectedItemId = itemId, startupLatencyMs = null) }
        }

        override fun preload(nextVideoUrls: List<String>) {
            preloadCalls.add(nextVideoUrls)
        }

        override fun pause() {
            playbackStateFlow.update { it.copy(isPlaying = false) }
        }

        override fun resume() {
            playbackStateFlow.update { it.copy(isPlaying = true) }
        }

        override fun release() {
            playbackStateFlow.update { it.copy(isPlaying = false) }
        }
    }
}
