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
            log("FAIL — SDK error: ${textOf(errorNodes.first())}")
            fail("SDK returned error: ${textOf(errorNodes.first())}")
        }
        composeRule.onNodeWithTag(TestTags.TEXT_UID).assertIsDisplayed()
        log("AccountScreen visible with UID")
    }

    /** Reads the visible UID string from the AccountScreen node (strips the "UID: " prefix). */
    private fun readUid(): String {
        val node = composeRule.onAllNodes(hasTestTag(TestTags.TEXT_UID)).fetchSemanticsNodes().first()
        return textOf(node).substringAfter("UID:").trim()
    }

    /** Waits for and returns the error status text, failing if none appears. */
    private fun waitForErrorStatus(timeoutMs: Long = 45_000): String {
        composeRule.waitUntil(timeoutMillis = timeoutMs) {
            composeRule.onAllNodes(hasTestTag(TestTags.TEXT_STATUS))
                .fetchSemanticsNodes().isNotEmpty()
        }
        return textOf(
            composeRule.onAllNodes(hasTestTag(TestTags.TEXT_STATUS)).fetchSemanticsNodes().first()
        )
    }

    private fun textOf(node: androidx.compose.ui.semantics.SemanticsNode): String =
        node.config
            .getOrElse(androidx.compose.ui.semantics.SemanticsProperties.Text) { emptyList() }
            .joinToString()

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
     * Test 02 — Login with the account created in test 01, verify AccountScreen with a valid
     * UID (data integrity), logout. Depends on [testEmail]/[testPassword] set by test 01.
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

        // Data integrity — the account must carry a non-empty UID, and we capture it
        // so test03 can assert the SAME account is restored after a relaunch.
        val uid = readUid()
        if (uid.isBlank()) fail("Login succeeded but account UID is blank — data integrity failure")
        testUid = uid
        log("PASS AccountScreen shown after login | uid=$uid")

        logout()
        log("PASS test02_login_withRegisteredAccount_andLogsOut")
    }

    /**
     * Test 03 — Session persistence: log in, relaunch the activity, and assert the session is
     * restored (AccountScreen shown with the SAME UID) without re-entering credentials.
     * Core backend-sanity signal: session token storage, encryption, and restoration.
     */
    @Test
    fun test03_sessionPersists_acrossRelaunch() {
        val email = testEmail
        val password = testPassword
        if (email.isBlank() || password.isBlank()) {
            fail("test03 depends on test01 — credentials not set. Run the full suite.")
            return
        }

        log("START test03_sessionPersists | email=$email")

        // Log in
        waitForLoginScreen()
        composeRule.onNodeWithTag(TestTags.INPUT_EMAIL).performTextInput(email)
        composeRule.onNodeWithTag(TestTags.INPUT_PASSWORD).performTextInput(password)
        composeRule.onNodeWithTag(TestTags.BTN_LOGIN).performClick()
        waitForAccountScreenOrFail()
        val uidBefore = readUid()
        log("Logged in | uid=$uidBefore — relaunching activity...")

        // Relaunch the activity — a valid session must route straight to AccountScreen
        composeRule.activityRule.scenario.recreate()

        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodes(hasTestTag(TestTags.TEXT_UID))
                .fetchSemanticsNodes().isNotEmpty()
        }
        val uidAfter = readUid()
        if (uidAfter != uidBefore) {
            fail("Session not restored correctly: uid before=$uidBefore after=$uidAfter")
        }
        log("PASS session restored after relaunch | uid=$uidAfter")

        logout()
        log("PASS test03_sessionPersists_acrossRelaunch")
    }

    /**
     * Test 04 — Logout invalidates the session: after logout, a relaunch must land on
     * LoginScreen (session truly cleared from storage, not just the UI reset).
     */
    @Test
    fun test04_logoutInvalidatesSession_acrossRelaunch() {
        val email = testEmail
        val password = testPassword
        if (email.isBlank() || password.isBlank()) {
            fail("test04 depends on test01 — credentials not set. Run the full suite.")
            return
        }

        log("START test04_logoutInvalidatesSession | email=$email")

        // Log in then log out
        waitForLoginScreen()
        composeRule.onNodeWithTag(TestTags.INPUT_EMAIL).performTextInput(email)
        composeRule.onNodeWithTag(TestTags.INPUT_PASSWORD).performTextInput(password)
        composeRule.onNodeWithTag(TestTags.BTN_LOGIN).performClick()
        waitForAccountScreenOrFail()
        logout()
        log("Logged out — relaunching activity...")

        // Relaunch — with no session we must land on LoginScreen
        composeRule.activityRule.scenario.recreate()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag(TestTags.BTN_REGISTER))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(TestTags.BTN_REGISTER).assertIsDisplayed()
        log("PASS LoginScreen shown after relaunch — session cleared")
        log("PASS test04_logoutInvalidatesSession_acrossRelaunch")
    }

    /**
     * Test 05 — Invalid credentials produce an error, not a silent hang or a false success.
     * Validates negative-path error propagation from backend → repository → UI.
     */
    @Test
    fun test05_login_withInvalidCredentials_showsError() {
        val badEmail = "nonexistent_${System.currentTimeMillis()}@gigya-test.com"
        val badPassword = "wrongPassword_${System.currentTimeMillis()}"

        log("START test05_invalidCredentials | email=$badEmail")

        waitForLoginScreen()
        composeRule.onNodeWithTag(TestTags.INPUT_EMAIL).performTextInput(badEmail)
        composeRule.onNodeWithTag(TestTags.INPUT_PASSWORD).performTextInput(badPassword)
        composeRule.onNodeWithTag(TestTags.BTN_LOGIN).performClick()
        log("Login tapped with bad credentials — waiting for error...")

        val error = waitForErrorStatus()
        if (error.isBlank()) fail("Expected an error for invalid credentials but status was blank")

        // Must NOT have navigated to AccountScreen
        composeRule.onAllNodes(hasTestTag(TestTags.TEXT_UID)).fetchSemanticsNodes().let {
            if (it.isNotEmpty()) fail("Invalid credentials unexpectedly reached AccountScreen")
        }
        log("PASS invalid credentials produced error: $error")
        log("PASS test05_login_withInvalidCredentials_showsError")
    }

    companion object {
        private const val TAG = "SanityTest"

        // Shared state between sequential tests — set by test01, read by later tests
        var testEmail: String = ""
        var testPassword: String = ""
        var testUid: String = ""
    }
}
