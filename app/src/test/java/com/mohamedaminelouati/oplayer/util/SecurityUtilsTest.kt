package com.mohamedaminelouati.oplayer.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityUtilsTest {

    @Test
    fun hashSecretProducesValidSha256HexDigest() {
        val hash = SecurityUtils.hashSecret("1234")
        assertTrue(hash.matches(Regex("[0-9a-f]{64}")))
    }

    @Test
    fun hashSecretIsDeterministicAndInputSensitive() {
        assertEquals(SecurityUtils.hashSecret("1234"), SecurityUtils.hashSecret("1234"))
        assertNotEquals(SecurityUtils.hashSecret("1234"), SecurityUtils.hashSecret("5678"))
    }

    @Test
    fun verifySecretValidatesCorrectAndRejectsInvalidSecrets() {
        val hash = SecurityUtils.hashSecret("9876")
        assertTrue(SecurityUtils.verifySecret("9876", hash))
        assertFalse(SecurityUtils.verifySecret("0000", hash))
        assertFalse(SecurityUtils.verifySecret("", hash))
        assertFalse(SecurityUtils.verifySecret("9876", ""))
    }
}
