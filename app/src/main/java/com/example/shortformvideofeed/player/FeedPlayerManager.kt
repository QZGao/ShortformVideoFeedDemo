package com.example.shortformvideofeed.player

import android.content.Context
import android.os.SystemClock
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class PlaybackUiState(
    val selectedItemId: String? = null,
    val playbackState: Int = Player.STATE_IDLE,
    val isBuffering: Boolean = false,
    val isPlaying: Boolean = false,
    val lastError: String? = null,
    val startupLatencyMs: Long? = null
)

interface FeedPlayerController {
    val playbackState: StateFlow<PlaybackUiState>
    fun activate(itemId: String?, videoUrl: String?, itemDurationMs: Long? = null)
    fun preload(nextVideoUrls: List<String>)
    fun pause()
    fun resume()
    fun release()
}

class FeedPlayerManager(
    context: Context,
    private val playbackPositionStore: PlaybackPositionStore = SharedPreferencesPlaybackPositionStore(context)
) : FeedPlayerController {
    private val appContext = context.applicationContext
    val player = ExoPlayer.Builder(appContext).build()

    private val preloadPlayers = List(2) { ExoPlayer.Builder(appContext).build() }
    private val _playbackState = MutableStateFlow(PlaybackUiState())
    override val playbackState: StateFlow<PlaybackUiState> = _playbackState

    private var selectedItemId: String? = null
    private var selectedAtMs: Long = 0L
    private var expectingFirstFrameForId: String? = null
    private val playbackPositionByItemId = LinkedHashMap<String, Long>()

    init {
        player.addListener(
            object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    _playbackState.update {
                        it.copy(
                            playbackState = playbackState,
                            isBuffering = playbackState == Player.STATE_BUFFERING,
                            isPlaying = it.isPlaying
                        )
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _playbackState.update { it.copy(isPlaying = isPlaying) }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    persistCurrentPosition(selectedItemId)
                    _playbackState.update {
                        it.copy(
                            lastError = error.message ?: "Playback error",
                            isPlaying = false,
                            isBuffering = false
                        )
                    }
                }

                override fun onRenderedFirstFrame() {
                    val itemId = expectingFirstFrameForId ?: return
                    val latency = SystemClock.elapsedRealtime() - selectedAtMs
                    _playbackState.update {
                        if (it.selectedItemId != itemId) it else it.copy(startupLatencyMs = latency)
                    }
                    expectingFirstFrameForId = null
                }
            }
        )
    }

    override fun activate(itemId: String?, videoUrl: String?, itemDurationMs: Long?) {
        _playbackState.update { it.copy(lastError = null) }
        val previouslySelectedItemId = selectedItemId
        if (previouslySelectedItemId != null && previouslySelectedItemId != itemId) {
            persistCurrentPosition(previouslySelectedItemId)
        }

        if (itemId == null || videoUrl.isNullOrBlank()) {
            pause()
            selectedItemId = null
            expectingFirstFrameForId = null
            _playbackState.update { it.copy(selectedItemId = null) }
            return
        }

        if (selectedItemId == itemId) {
            if (player.playbackState == Player.STATE_ENDED) {
                player.seekTo(0L)
            } else if (player.playbackState != Player.STATE_IDLE) {
                player.play()
                return
            }
        }

        selectedItemId = itemId
        selectedAtMs = SystemClock.elapsedRealtime()
        expectingFirstFrameForId = itemId
        val resumePositionMs = playbackPositionStore.load(itemId) ?: playbackPositionByItemId[itemId]
        val durationMs = itemDurationMs ?: 0L
        val startPositionMs = resumePositionMs
            ?.takeIf { it > 0L && durationMs > 0L && it < durationMs - 500L }
            ?: 0L
        val mediaItem = MediaItem.fromUri(videoUrl)
        if (startPositionMs > 0L) {
            player.setMediaItem(mediaItem, startPositionMs)
        } else {
            player.setMediaItem(mediaItem)
        }
        player.prepare()
        player.play()
        _playbackState.update {
            it.copy(
                selectedItemId = itemId,
                playbackState = Player.STATE_BUFFERING,
                isBuffering = true,
                isPlaying = true,
                startupLatencyMs = null
            )
        }
    }

    override fun preload(nextVideoUrls: List<String>) {
        val sanitized = nextVideoUrls
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filter { it != player.currentMediaItem?.localConfiguration?.uri?.toString() }

        if (sanitized.isEmpty()) {
            preloadPlayers.forEach { it.clearMediaItems() }
            return
        }

        preloadPlayers.forEachIndexed { index, player ->
            val url = sanitized.getOrNull(index)
            if (url == null) {
                player.clearMediaItems()
                return@forEachIndexed
            }
            if (player.currentMediaItem?.localConfiguration?.uri?.toString() == url) return@forEachIndexed
            player.clearMediaItems()
            player.setMediaItem(MediaItem.fromUri(url))
            player.prepare()
        }
    }

    override fun pause() {
        persistCurrentPosition(selectedItemId)
        player.pause()
    }

    override fun resume() {
        player.play()
    }

    override fun release() {
        persistCurrentPosition(selectedItemId)
        player.release()
        preloadPlayers.forEach { it.release() }
    }

    private fun persistCurrentPosition(itemId: String?) {
        if (itemId == null) return
        val positionMs = player.currentPosition
        val durationMs = player.duration
        if (durationMs > 0 && positionMs >= durationMs) return
        playbackPositionByItemId[itemId] = positionMs
        playbackPositionStore.save(itemId, positionMs)
    }
}
