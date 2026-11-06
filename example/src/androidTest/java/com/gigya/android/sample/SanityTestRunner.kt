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
 * Also reads TFA extension credentials from secrets.xml and stores them in [Secrets] so
 * test suites can access them without embedding secrets in source code.
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

        // Load TFA extension credentials into the Secrets holder.
        Secrets.tfaApiKey         = ctx.getString(R.string.tfa_api_key)
        Secrets.tfaApiDomain      = ctx.getString(R.string.tfa_api_domain)
        Secrets.extensionUrl      = ctx.getString(R.string.tfa_extension_url)
        Secrets.extensionToken    = ctx.getString(R.string.tfa_extension_token)

        super.onCreate(arguments)
    }

    /** Holds run-time secrets read from secrets.xml — never hardcoded in source. */
    object Secrets {
        var tfaApiKey: String = ""
        var tfaApiDomain: String = "us1.gigya.com"
        var extensionUrl: String = ""
        var extensionToken: String = ""
    }
}
