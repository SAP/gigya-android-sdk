package com.gigya.android.sample

import android.Manifest
import android.os.Build
import android.util.Log
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.gigya.android.sample.ui.MainActivity
import com.gigya.android.sample.ui.common.TestTags
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

/**
 * Sanity E2E tests — drives the real Compose UI against the live Gigya site.
 *
 * Credentials are read at runtime from the gitignored secrets.xml via the
 * app context — single source of truth, no BuildConfig coupling.
 *
 * Local run:  ./gradlew :example:connectedAndroidTest
 *             adb logcat -s SanityTest   ← live step-by-step progress
 *
 * Requires a connected device or running emulator with secrets.xml present.
 *
 * GitHub Actions: deferred — CI will use a login-only variant with a
 * fixed pre-existing test account (register not allowed in CI).
 */
@RunWith(AndroidJUnit4::class)
class SanityTest {

    // Grant POST_NOTIFICATIONS upfront so the permission dialog doesn't pause the activity
    @get:Rule(order = 0)
    val permissionRule: GrantPermissionRule = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        GrantPermissionRule.grant()
    }

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val emailPrefix get() = context.getString(R.string.test_email_prefix)

    private fun generateEmail() = "${emailPrefix}_${System.currentTimeMillis()}@gigya-test.com"

    private fun generatePassword(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..12).map { chars.random() }.joinToString("") + "Aa1!"
    }

    /**
     * Sanity: register a new account, verify the account screen shows a UID,
     * then logout and verify the login screen is restored.
     *
     * A unique email is generated per run — no account cleanup required.
     * Follow progress live: adb logcat -s SanityTest
     */
    @Test
    fun register_showsAccountScreen_andLogoutReturnsToLogin() {
        val email = generateEmail()
        val password = generatePassword()
        log("START register_showsAccountScreen_andLogoutReturnsToLogin")
        log("Generated test email: $email")

        // Wait for LoginScreen to settle after splash screen
        log("Waiting for LoginScreen...")
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag(TestTags.BTN_REGISTER))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(TestTags.BTN_REGISTER).assertIsDisplayed()
        log("STEP 1 PASS — LoginScreen visible")

        // Enter credentials and register
        log("Entering credentials and tapping Register...")
        composeRule.onNodeWithTag(TestTags.INPUT_EMAIL).performTextInput(email)
        composeRule.onNodeWithTag(TestTags.INPUT_PASSWORD).performTextInput(password)
        composeRule.onNodeWithTag(TestTags.BTN_REGISTER).performClick()
        log("STEP 2 — Register tapped, waiting for API response (up to 45s)...")

        // Wait for either AccountScreen (success) or error status (failure)
        composeRule.waitUntil(timeoutMillis = 45_000) {
            val hasUid = composeRule.onAllNodes(hasTestTag(TestTags.TEXT_UID))
                .fetchSemanticsNodes().isNotEmpty()
            val hasError = composeRule.onAllNodes(hasTestTag(TestTags.TEXT_STATUS))
                .fetchSemanticsNodes().isNotEmpty()
            hasUid || hasError
        }

        // Fail fast with a descriptive message if the error status is shown
        val errorNodes = composeRule.onAllNodes(hasTestTag(TestTags.TEXT_STATUS))
            .fetchSemanticsNodes()
        if (errorNodes.isNotEmpty()) {
            val errorText = errorNodes.first()
                .config
                .getOrElse(androidx.compose.ui.semantics.SemanticsProperties.Text) { emptyList() }
                .joinToString()
            log("STEP 3 FAIL — Registration error: $errorText")
            fail("Registration failed with error: $errorText")
        }

        // AccountScreen is shown with a UID
        composeRule.onNodeWithTag(TestTags.TEXT_UID).assertIsDisplayed()
        log("STEP 3 PASS — AccountScreen visible with UID")

        // Logout
        log("Tapping Logout...")
        composeRule.onNodeWithTag(TestTags.BTN_LOGOUT).performClick()

        // Wait for LoginScreen to be restored
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag(TestTags.BTN_REGISTER))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(TestTags.BTN_REGISTER).assertIsDisplayed()
        log("STEP 4 PASS — LoginScreen restored after logout")
        log("PASS register_showsAccountScreen_andLogoutReturnsToLogin")
    }

    private fun log(message: String) = Log.d(TAG, message)

    companion object {
        private const val TAG = "SanityTest"
    }
}
