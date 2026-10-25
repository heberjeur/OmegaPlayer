package com.arslandaim.omegaplayer.util

import java.security.MessageDigest

object SecurityUtils {
    private const val HASH_SALT = "OmegaPlayer_Lock_Salt_2026"

    fun hashSecret(secret: String): String {
        val input = secret + HASH_SALT
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun verifySecret(input: String, storedHash: String): Boolean {
        if (input.isEmpty() || storedHash.isEmpty()) return false
        return hashSecret(input) == storedHash
    }
}
