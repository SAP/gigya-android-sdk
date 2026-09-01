package com.gigya.android.sample

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.gigya.android.sample.ui.MainActivity
import com.gigya.android.sample.ui.common.TestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

/**
 * Sanity E2E tests — drive the real Compose UI against the live Gigya site.
 *
 * Credentials are read at runtime from the gitignored secrets.xml via the
 * app context — single source of truth, no BuildConfig coupling.
 *
 * Local run: ./gradlew :example:connectedAndroidTest
 * Requires a connected device or running emulator with secrets.xml present.
 *
 * GitHub Actions: deferred — CI will use a login-only variant with a fixed
 * pre-existing test account (register not allowed in CI).
 */
@RunWith(AndroidJUnit4::class)
class SanityTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val emailPrefix: String
        get() = context.getString(R.string.test_email_prefix)

    private fun generateEmail() =
        "${emailPrefix}_${System.currentTimeMillis()}@gigya-test.com"

    private fun generatePassword(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..12).map { chars.random() }.joinToString("") + "1aA!"
    }

    /**
     * Sanity: register a new account, verify the account screen is shown with a UID,
     * then logout and verify the login screen is restored.
     *
     * A unique email is generated per run — no cleanup required.
     */
    @Test
    fun register_showsAccountScreen_andLogoutReturnsToLogin() {
        val email = generateEmail()
        val password = generatePassword()

        // LoginScreen should be visible on launch
        composeRule.onNodeWithTag(TestTags.BTN_REGISTER).assertIsDisplayed()

        // Enter credentials
        composeRule.onNodeWithTag(TestTags.INPUT_EMAIL).performTextInput(email)
        composeRule.onNodeWithTag(TestTags.INPUT_PASSWORD).performTextInput(password)

        // Tap Register
        composeRule.onNodeWithTag(TestTags.BTN_REGISTER).performClick()

        // Wait for AccountScreen — UID text node must appear (network call)
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodes(
                androidx.compose.ui.test.hasTestTag(TestTags.TEXT_UID)
            ).fetchSemanticsNodes().isNotEmpty()
        }

        // Assert AccountScreen is shown with a non-empty UID
        composeRule.onNodeWithTag(TestTags.TEXT_UID).assertIsDisplayed()

        // Logout
        composeRule.onNodeWithTag(TestTags.BTN_LOGOUT).performClick()

        // Wait for LoginScreen to be restored
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(
                androidx.compose.ui.test.hasTestTag(TestTags.BTN_REGISTER)
            ).fetchSemanticsNodes().isNotEmpty()
        }

        // Assert we are back on LoginScreen
        composeRule.onNodeWithTag(TestTags.BTN_REGISTER).assertIsDisplayed()
    }
}
