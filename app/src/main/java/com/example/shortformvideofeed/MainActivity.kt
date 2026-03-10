package com.example.shortformvideofeed

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.shortformvideofeed.app.AppContainer
import com.example.shortformvideofeed.domain.usecase.ObserveFeedUseCase
import com.example.shortformvideofeed.domain.usecase.ObservePagedFeedUseCase
import com.example.shortformvideofeed.domain.usecase.RefreshFeedUseCase
import com.example.shortformvideofeed.player.FeedPlayerManager
import com.example.shortformvideofeed.player.SharedPreferencesPlaybackPositionStore
import com.example.shortformvideofeed.ui.feed.FeedScreen
import com.example.shortformvideofeed.ui.feed.FeedViewModel
import com.example.shortformvideofeed.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    private lateinit var playerManager: FeedPlayerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = AppContainer(application)
        playerManager = FeedPlayerManager(
            context = applicationContext,
            playbackPositionStore = SharedPreferencesPlaybackPositionStore(applicationContext)
        )

        setContent {
            val feedViewModel = FeedViewModel(
                observeFeedUseCase = ObserveFeedUseCase(container.feedRepository),
                observePagedFeedUseCase = ObservePagedFeedUseCase(container.feedRepository),
                refreshFeedUseCase = RefreshFeedUseCase(container.feedRepository),
                playerManager = playerManager
            )

            AppTheme {
                FeedScreen(
                    viewModel = feedViewModel,
                    playerManager = playerManager
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        playerManager.pause()
    }

    override fun onResume() {
        super.onResume()
        playerManager.resume()
    }

    override fun onDestroy() {
        playerManager.release()
        super.onDestroy()
    }
}
