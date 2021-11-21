package com.gigya.android.sdk.utils

import android.util.Base64
import com.gigya.android.sdk.GigyaLogger
import java.security.MessageDigest
import java.security.SecureRandom

class PKCEHelper {

    var verifier: String? = null
    var challenge: String? = null

    fun newChallenge() {
        verifier = generateCodeVerifier()
        challenge = generateCodeChallenge(verifier!!)
    }

    private fun generateCodeVerifier(): String {
        val sr = SecureRandom()
        val code = ByteArray(32)
        sr.nextBytes(code)
        val v = Base64.encodeToString(code, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        GigyaLogger.debug("PKCE", "v:$v")
        return v
    }

    private fun generateCodeChallenge(verifier: String): String {
        val bytes: ByteArray = verifier.toByteArray(charset("UTF-8"))
        val md = MessageDigest.getInstance("SHA-256")
        md.update(bytes, 0, bytes.size)
        val digest = md.digest()
        val c = Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        GigyaLogger.debug("PKCE", "c:$c")
        return c
    }
}