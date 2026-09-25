package com.softhome.feature.home

import kotlinx.coroutines.flow.StateFlow

/** UI-facing playback state. A future MediaSession adapter can implement the same contract. */
data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentTimeMs: Long = 0L,
    val durationMs: Long = 210_000L,
) {
    val progress: Float
        get() = if (durationMs <= 0L) 0f else (currentTimeMs.toFloat() / durationMs).coerceIn(0f, 1f)
}

/** Playback boundary consumed by the UI; it deliberately contains no Spotify/MediaSession types. */
interface PlaybackController {
    val state: StateFlow<PlaybackState>

    fun togglePlayPause()
    fun seekTo(positionMs: Long)
}

/**
 * Deterministic local playback implementation for the design/demo player.
 * The UI advances it in fixed 50ms steps while playing, so pause/resume/seek are predictable.
 */
class DemoPlaybackController : PlaybackController {
    private val _state = kotlinx.coroutines.flow.MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state

    override fun togglePlayPause() {
        _state.value = _state.value.copy(isPlaying = !_state.value.isPlaying)
    }

    override fun seekTo(positionMs: Long) {
        val duration = _state.value.durationMs
        _state.value = _state.value.copy(currentTimeMs = positionMs.coerceIn(0L, duration))
    }

    /** Advances only while playing; intended to be called by the Compose ticker. */
    fun advanceBy(deltaMs: Long) {
        val current = _state.value
        if (!current.isPlaying || deltaMs <= 0L) return
        val next = current.currentTimeMs + deltaMs
        if (next >= current.durationMs) {
            _state.value = current.copy(currentTimeMs = current.durationMs, isPlaying = false)
        } else {
            _state.value = current.copy(currentTimeMs = next)
        }
    }
}
