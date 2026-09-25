package com.arslandaim.omegaplayer.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityUtilsTest {

    @Test
    fun hashStringMatchesKnownSha256Vectors() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            SecurityUtils.hashString("abc")
        )
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            SecurityUtils.hashString("")
        )
    }

    @Test
    fun hashStringAlwaysProducesLowercaseHexDigest() {
        assertTrue(SecurityUtils.hashString("OmegaPlayer").matches(Regex("[0-9a-f]{64}")))
    }

    @Test
    fun hashStringIsDeterministicAndInputSensitive() {
        assertEquals(SecurityUtils.hashString("same input"), SecurityUtils.hashString("same input"))
        assertNotEquals(SecurityUtils.hashString("input a"), SecurityUtils.hashString("input b"))
    }
}
