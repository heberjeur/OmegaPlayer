package com.arslandaim.omegaplayer.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

class MediaUtilsTest {

    private lateinit var defaultLocale: Locale

    @Before
    fun setUp() {
        defaultLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun formatDurationRendersMinutesAndSeconds() {
        assertEquals("00:00", MediaUtils.formatDuration(0L))
        assertEquals("00:59", MediaUtils.formatDuration(59_999L))
        assertEquals("01:00", MediaUtils.formatDuration(60_000L))
        assertEquals("59:59", MediaUtils.formatDuration(3_599_000L))
    }

    @Test
    fun formatDurationRendersHoursWhenPresent() {
        assertEquals("1:00:00", MediaUtils.formatDuration(3_600_000L))
        assertEquals("1:01:01", MediaUtils.formatDuration(3_661_000L))
        assertEquals("10:00:00", MediaUtils.formatDuration(36_000_000L))
    }

    @Test
    fun formatDurationHandlesUnsetAndNegativeValues() {
        assertEquals("00:00", MediaUtils.formatDuration(-9223372036854775807L))
        assertEquals("01:30", MediaUtils.formatDuration(-90_000L))
    }

    @Test
    fun safeEncodeUriEncodesRawContentUri() {
        assertEquals(
            "content%3A%2F%2Fmedia%2Fexternal%2Fvideo%2Fmedia%2F42",
            MediaUtils.safeEncodeUri("content://media/external/video/media/42")
        )
    }

    @Test
    fun safeEncodeUriKeepsAlreadyEncodedUriStable() {
        assertEquals("content%3A%2F%2Fmedia", MediaUtils.safeEncodeUri("content%3A%2F%2Fmedia"))
    }

    @Test
    fun safeDecodeUriDecodesEncodedContentUri() {
        assertEquals("content://media/external", MediaUtils.safeDecodeUri("content%3A%2F%2Fmedia%2Fexternal"))
        assertEquals("plain", MediaUtils.safeDecodeUri("plain"))
    }

    @Test
    fun encodeDecodeRoundTripRestoresOriginalUri() {
        val uri = "content://media/external/audio/media/7"
        assertEquals(uri, MediaUtils.safeDecodeUri(MediaUtils.safeEncodeUri(uri)))
    }
}
