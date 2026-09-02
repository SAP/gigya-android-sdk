package com.gigya.android.sample

import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner
import com.gigya.android.sdk.Gigya

/**
 * Custom instrumentation runner for the sanity E2E suite.
 *
 * Sets a programmatic Gigya configuration BEFORE [Application.onCreate] runs, so the SDK
 * initializes against the test site from secrets.xml rather than reading
 * gigyaSdkConfiguration.json from assets. This makes SDK init deterministic and avoids the
 * race between JSON auto-init and any explicit re-init.
 *
 * Wired via `testInstrumentationRunner` in example/build.gradle.
 */
class SanityTestRunner : AndroidJUnitRunner() {

    override fun onCreate(arguments: Bundle?) {
        // Runs before the app Application.onCreate — set config here so the SDK's implicit
        // init picks up the programmatic config instead of the JSON assets file.
        val ctx = targetContext
        val apiKey = ctx.getString(R.string.gigya_api_key)
        val apiDomain = ctx.getString(R.string.gigya_api_domain)
        Gigya.setConfiguration(apiKey, apiDomain)
        super.onCreate(arguments)
    }
}
