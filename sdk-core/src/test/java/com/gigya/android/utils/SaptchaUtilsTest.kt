package com.gigya.android.utils

import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.auth.SaptchaUtils

// File: src/test/java/com/gigya/android/sdk/auth/SaptchaUtilsTest.kt

import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SaptchaUtilsTest {

    private lateinit var saptchaUtils: SaptchaUtils

    @Before
    fun setUp() {
        saptchaUtils = SaptchaUtils()
    }

    @Test
    fun testVerifyChallenge() {
        val challengeId = "testChallengeId"
        val pattern = "^.{54}a60" // Example pattern for SHA-512 hash
        val i = 1


        val value = "db09666069e26747848fffb29d7000167bd4bfa0204b85a48dae7ea60937e1b9062525cf77822564ca868c419bd8e8f472fea3bf9dca3c466a5455a051025bae"
        val regex = pattern.toRegex()
        val matches = regex.containsMatchIn(value)
    }
}