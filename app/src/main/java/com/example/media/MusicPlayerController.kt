package com.example.media

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class AudioTrackItem(
    val uri: Uri,
    val title: String,
    val sizeBytes: Long,
    val mimeType: String?
)

data class MusicPlayerUiState(
    val playlist: List<AudioTrackItem> = emptyList(),
    val currentTrackIndex: Int = -1,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val volume: Float = 0.85f,
    val errorMessage: String? = null
) {
    val currentTrack: AudioTrackItem?
        get() = playlist.getOrNull(currentTrackIndex)
}

class MusicPlayerController(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var exoPlayer: ExoPlayer? = null
    private var progressJob: Job? = null

    private val _uiState = MutableStateFlow(MusicPlayerUiState())
    val uiState: StateFlow<MusicPlayerUiState> = _uiState.asStateFlow()

    private fun getOrCreatePlayer(): ExoPlayer {
        val existing = exoPlayer
        if (existing != null) return existing
        return ExoPlayer.Builder(context).build().also { player ->
            player.volume = _uiState.value.volume
            player.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _uiState.update {
                        it.copy(
                            isPlaying = isPlaying,
                            durationMs = player.duration.coerceAtLeast(0L)
                        )
                    }
                    if (isPlaying) startProgressPolling() else stopProgressPolling()
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    _uiState.update {
                        it.copy(
                            currentTrackIndex = player.currentMediaItemIndex,
                            durationMs = player.duration.coerceAtLeast(0L),
                            currentPositionMs = player.currentPosition.coerceAtLeast(0L),
                            errorMessage = null
                        )
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    _uiState.update {
                        it.copy(
                            durationMs = player.duration.coerceAtLeast(0L),
                            currentPositionMs = player.currentPosition.coerceAtLeast(0L)
                        )
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    _uiState.update {
                        it.copy(
                            isPlaying = false,
                            errorMessage = "Audio playback error: ${error.localizedMessage ?: "Unsupported audio codec or disconnected USB drive"}"
                        )
                    }
                }
            })
            exoPlayer = player
        }
    }

    fun setPlaylist(tracks: List<AudioTrackItem>, autoPlayIndex: Int = -1) {
        _uiState.update {
            it.copy(
                playlist = tracks,
                currentTrackIndex = if (tracks.isEmpty()) -1 else autoPlayIndex.coerceIn(0, tracks.lastIndex),
                errorMessage = null
            )
        }
        val player = getOrCreatePlayer()
        player.stop()
        player.clearMediaItems()
        tracks.forEach { track ->
            player.addMediaItem(MediaItem.fromUri(track.uri))
        }
        if (tracks.isNotEmpty() && autoPlayIndex in tracks.indices) {
            player.seekTo(autoPlayIndex, 0L)
            player.prepare()
            player.play()
        } else if (tracks.isNotEmpty()) {
            _uiState.update { it.copy(currentTrackIndex = 0) }
            player.prepare()
        }
    }

    fun playTrackAt(index: Int) {
        val tracks = _uiState.value.playlist
        if (index !in tracks.indices) return
        val player = getOrCreatePlayer()
        if (player.mediaItemCount != tracks.size) {
            setPlaylist(tracks, autoPlayIndex = index)
            return
        }
        _uiState.update { it.copy(currentTrackIndex = index, errorMessage = null) }
        player.seekTo(index, 0L)
        player.prepare()
        player.play()
    }

    fun togglePlayPause() {
        val tracks = _uiState.value.playlist
        if (tracks.isEmpty()) return
        val player = getOrCreatePlayer()
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_IDLE) {
                player.prepare()
            } else if (player.playbackState == Player.STATE_ENDED) {
                player.seekTo(player.currentMediaItemIndex, 0L)
            }
            player.play()
        }
    }

    fun skipNext() {
        val tracks = _uiState.value.playlist
        if (tracks.isEmpty()) return
        val nextIndex = (_uiState.value.currentTrackIndex + 1) % tracks.size
        playTrackAt(nextIndex)
    }

    fun skipPrevious() {
        val tracks = _uiState.value.playlist
        if (tracks.isEmpty()) return
        val prevIndex = if (_uiState.value.currentTrackIndex - 1 < 0) {
            tracks.lastIndex
        } else {
            _uiState.value.currentTrackIndex - 1
        }
        playTrackAt(prevIndex)
    }

    fun seekTo(positionMs: Long) {
        val player = exoPlayer ?: return
        val target = positionMs.coerceAtLeast(0L)
        player.seekTo(target)
        _uiState.update { it.copy(currentPositionMs = target) }
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        exoPlayer?.volume = clamped
        _uiState.update { it.copy(volume = clamped) }
    }

    private fun startProgressPolling() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val player = exoPlayer
                if (player != null && player.isPlaying) {
                    _uiState.update {
                        it.copy(
                            currentPositionMs = player.currentPosition.coerceAtLeast(0L),
                            durationMs = player.duration.coerceAtLeast(0L)
                        )
                    }
                }
                delay(250L)
            }
        }
    }

    private fun stopProgressPolling() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stopProgressPolling()
        exoPlayer?.release()
        exoPlayer = null
    }
}
