package com.example.shortformvideofeed.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.LoadState
import coil.compose.AsyncImage
import com.example.shortformvideofeed.domain.model.PreloadMode
import com.example.shortformvideofeed.player.FeedPlayerManager
import com.example.shortformvideofeed.player.PlaybackUiState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
fun FeedScreen(viewModel: FeedViewModel, playerManager: FeedPlayerManager) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val playbackState by playerManager.playbackState.collectAsState()
    val pagingItems = viewModel.pagedFeed.collectAsLazyPagingItems()

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .map { it.coerceAtLeast(0) }
            .distinctUntilChanged()
            .collect { index -> viewModel.onActiveItemChanged(index) }
    }

    LaunchedEffect(state.activeItemIndex) {
        if (state.activeItemIndex < pagingItems.itemCount) {
            listState.scrollToItem(state.activeItemIndex)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                (state.isLoading || pagingItems.loadState.refresh is LoadState.Loading) && pagingItems.itemCount == 0 -> {
                    LoadingState()
                }
                pagingItems.itemCount == 0 -> {
                    val errorMessage = state.errorMessage
                    if (errorMessage != null) {
                        ErrorState(message = errorMessage, onRetry = { viewModel.onPullToRefresh() })
                    } else {
                        ErrorState(message = "No content available.", onRetry = { viewModel.onPullToRefresh() })
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            count = pagingItems.itemCount,
                            key = { index ->
                                pagingItems[index]?.id ?: "placeholder-$index"
                            }
                        ) { index ->
                            val item = pagingItems[index]
                            if (item == null) {
                                FeedPlaceholder()
                            } else {
                                FeedVideoItem(
                                    item = item,
                                    isActive = index == state.activeItemIndex,
                                    playerManager = playerManager,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight()
                                )
                            }
                        }
                    }
                }
            }

            state.errorMessage?.let { errorMessage ->
                if (pagingItems.itemCount > 0) {
                    NetworkErrorBanner(
                        message = errorMessage,
                        onRetry = { viewModel.onPullToRefresh() },
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }

            DebugOverlay(
                activeItemId = state.items.getOrNull(state.activeItemIndex)?.id ?: pagingItems[state.activeItemIndex]?.id,
                source = state.source.name,
                isBuffering = playbackState.isBuffering,
                playbackState = playbackState,
                preloadMode = state.preloadMode,
                onPreloadModeChange = { mode -> viewModel.onPreloadModeChanged(mode) },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .navigationBarsPadding()
                    .padding(12.dp)
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color.White)
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = message,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.size(8.dp))
            Button(onClick = onRetry) {
                Text(text = "Retry")
            }
        }
    }
}

@Composable
private fun NetworkErrorBanner(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = Modifier
            .then(modifier)
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xCC000000)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = message, color = Color.White, fontSize = 12.sp)
                Button(onClick = onRetry) {
                    Text(text = "Retry")
                }
            }
        }
    }
}

@Composable
private fun FeedVideoItem(
    item: com.example.shortformvideofeed.domain.model.VideoItem,
    isActive: Boolean,
    playerManager: FeedPlayerManager,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        if (isActive) {
            AndroidView(
                factory = { context ->
                    PlayerView(context).apply {
                        useController = false
                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) { playerView ->
                playerView.player = playerManager.player
            }
        } else {
            AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                maxLines = 1
            )
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 2
            )
        }
    }
}

@Composable
private fun FeedPlaceholder() {
    Box(modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight()
        .background(Color.DarkGray)
    )
}

@Composable
private fun DebugOverlay(
    activeItemId: String?,
    source: String,
    isBuffering: Boolean,
    playbackState: PlaybackUiState,
    preloadMode: PreloadMode,
    onPreloadModeChange: (PreloadMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = "Feed source: $source", color = Color.White, fontSize = 12.sp)
            Text(text = "Active item: ${activeItemId ?: "none"}", color = Color.White, fontSize = 12.sp)
            Text(
                text = "Player: ${playbackStateLabel(playbackState.playbackState)} | " +
                    "buffering=$isBuffering | " +
                    "first-frame=${playbackState.startupLatencyMs?.let { "${it}ms" } ?: "-"}",
                color = Color.White,
                fontSize = 12.sp,
                maxLines = 2
            )
            playbackState.lastError?.let {
                Text(text = "Playback error: $it", color = Color(0xFFFF6E6E), fontSize = 12.sp)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PreloadMode.values().forEach { mode ->
                    OutlinedButton(
                        onClick = { onPreloadModeChange(mode) },
                        enabled = preloadMode != mode,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = mode.name)
                    }
                }
            }
        }
    }
}

private fun playbackStateLabel(state: Int): String {
    return when (state) {
        Player.STATE_READY -> "READY"
        Player.STATE_BUFFERING -> "BUFFERING"
        Player.STATE_ENDED -> "ENDED"
        Player.STATE_IDLE -> "IDLE"
        else -> "UNKNOWN"
    }
}
