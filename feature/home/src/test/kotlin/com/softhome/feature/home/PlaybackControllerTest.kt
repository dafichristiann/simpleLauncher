package com.softhome.feature.home

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlaybackControllerTest {
    @Test
    fun `demo playback advances pauses resumes and seeks deterministically`() {
        val controller = DemoPlaybackController()
        controller.togglePlayPause()
        controller.advanceBy(1_000L)
        assertThat(controller.state.value.currentTimeMs).isEqualTo(1_000L)

        controller.togglePlayPause()
        controller.advanceBy(2_000L)
        assertThat(controller.state.value.currentTimeMs).isEqualTo(1_000L)

        controller.seekTo(120_000L)
        assertThat(controller.state.value.progress).isWithin(0.001f).of(120_000f / 210_000f)
        controller.togglePlayPause()
        controller.advanceBy(500L)
        assertThat(controller.state.value.currentTimeMs).isEqualTo(120_500L)
    }

    @Test
    fun `demo playback clamps seek and stops at duration`() {
        val controller = DemoPlaybackController()
        controller.seekTo(Long.MAX_VALUE)
        assertThat(controller.state.value.currentTimeMs).isEqualTo(210_000L)
        controller.seekTo(0L)
        controller.togglePlayPause()
        controller.advanceBy(300_000L)
        assertThat(controller.state.value.currentTimeMs).isEqualTo(210_000L)
        assertThat(controller.state.value.isPlaying).isFalse()
    }
}
