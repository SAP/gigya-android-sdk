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
import org.junit.FixMethodOrder
import org.junit.runners.MethodSorters
import androidx.test.ext.junit.runners.AndroidJUnit4

/**
 * Sanity E2E test suite — sequential flows against the live Gigya site.
 *
 * Tests run in name order (01_, 02_, ...) and share account credentials
 * via [companion object] so each test builds on the previous one.
 * Each test gets a fresh Activity launch.
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
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class SanityTest {

    @get:Rule(order = 0)
    val permissionRule: GrantPermissionRule = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        GrantPermissionRule.grant()
    }

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    // region Helpers

    private fun waitForLoginScreen() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag(TestTags.BTN_REGISTER))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(TestTags.BTN_REGISTER).assertIsDisplayed()
        log("LoginScreen visible")
    }

    private fun waitForAccountScreenOrFail(timeoutMs: Long = 45_000) {
        composeRule.waitUntil(timeoutMillis = timeoutMs) {
            val hasUid = composeRule.onAllNodes(hasTestTag(TestTags.TEXT_UID))
                .fetchSemanticsNodes().isNotEmpty()
            val hasError = composeRule.onAllNodes(hasTestTag(TestTags.TEXT_STATUS))
                .fetchSemanticsNodes().isNotEmpty()
            hasUid || hasError
        }
        val errorNodes = composeRule.onAllNodes(hasTestTag(TestTags.TEXT_STATUS))
            .fetchSemanticsNodes()
        if (errorNodes.isNotEmpty()) {
            val errorText = errorNodes.first()
                .config
                .getOrElse(androidx.compose.ui.semantics.SemanticsProperties.Text) { emptyList() }
                .joinToString()
            log("FAIL — SDK error: $errorText")
            fail("SDK returned error: $errorText")
        }
        composeRule.onNodeWithTag(TestTags.TEXT_UID).assertIsDisplayed()
        log("AccountScreen visible with UID")
    }

    private fun logout() {
        composeRule.onNodeWithTag(TestTags.BTN_LOGOUT).performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag(TestTags.BTN_REGISTER))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(TestTags.BTN_REGISTER).assertIsDisplayed()
        log("LoginScreen restored after logout")
    }

    private fun log(message: String) = Log.d(TAG, message)

    // endregion

    /**
     * Test 01 — Register a new account, verify AccountScreen, logout.
     * Stores the generated credentials in [companion object] for test 02.
     */
    @Test
    fun test01_register_createsAccount_andLogsOut() {
        val email = "${context.getString(R.string.test_email_prefix)}_${System.currentTimeMillis()}@gigya-test.com"
        val password = buildString {
            val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
            repeat(12) { append(chars.random()) }
            append("Aa1!")
        }

        // Persist for test 02
        testEmail = email
        testPassword = password

        log("START test01_register | email=$email")

        waitForLoginScreen()

        log("Entering credentials and tapping Register...")
        composeRule.onNodeWithTag(TestTags.INPUT_EMAIL).performTextInput(email)
        composeRule.onNodeWithTag(TestTags.INPUT_PASSWORD).performTextInput(password)
        composeRule.onNodeWithTag(TestTags.BTN_REGISTER).performClick()
        log("Register tapped — waiting for API response...")

        waitForAccountScreenOrFail()
        log("PASS AccountScreen shown after register")

        logout()
        log("PASS test01_register_createsAccount_andLogsOut")
    }

    /**
     * Test 02 — Login with the account created in test 01, verify AccountScreen, logout.
     * Depends on [testEmail] and [testPassword] set by test 01.
     */
    @Test
    fun test02_login_withRegisteredAccount_andLogsOut() {
        val email = testEmail
        val password = testPassword

        if (email.isBlank() || password.isBlank()) {
            fail("test02 depends on test01 — testEmail/testPassword not set. Run the full suite.")
            return
        }

        log("START test02_login | email=$email")

        waitForLoginScreen()

        log("Entering credentials and tapping Login...")
        composeRule.onNodeWithTag(TestTags.INPUT_EMAIL).performTextInput(email)
        composeRule.onNodeWithTag(TestTags.INPUT_PASSWORD).performTextInput(password)
        composeRule.onNodeWithTag(TestTags.BTN_LOGIN).performClick()
        log("Login tapped — waiting for API response...")

        waitForAccountScreenOrFail()
        log("PASS AccountScreen shown after login")

        logout()
        log("PASS test02_login_withRegisteredAccount_andLogsOut")
    }

    companion object {
        private const val TAG = "SanityTest"

        // Shared state between sequential tests — set by test01, read by test02
        var testEmail: String = ""
        var testPassword: String = ""
    }
}
