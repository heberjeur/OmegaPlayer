package com.mohamedaminelouati.oplayer.ui.common

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarqueeTextTest {

    @Test
    fun testArabicTextDetectedAsRtl() {
        assertTrue(isRtlText("سورة البقرة", isLayoutRtl = false))
        assertTrue(isRtlText("فيديو جديد", isLayoutRtl = false))
        assertTrue(isRtlText("123 فيلم عربي", isLayoutRtl = false))
    }

    @Test
    fun testLatinTextDetectedAsLtr() {
        assertFalse(isRtlText("Video title 2026", isLayoutRtl = true))
        assertFalse(isRtlText("Documentary.mp4", isLayoutRtl = true))
        assertFalse(isRtlText("12345 English Track", isLayoutRtl = true))
    }

    @Test
    fun testNeutralTextFollowsLayoutDirection() {
        assertTrue(isRtlText("12345", isLayoutRtl = true))
        assertFalse(isRtlText("12345", isLayoutRtl = false))
        assertTrue(isRtlText("---", isLayoutRtl = true))
        assertFalse(isRtlText("---", isLayoutRtl = false))
    }
}
