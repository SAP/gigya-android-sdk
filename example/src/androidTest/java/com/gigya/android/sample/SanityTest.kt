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
 * Sanity E2E test suite — the backend-deployment sanity harness (CXCDC-44483).
 *
 * Drives the real Compose UI on a device/emulator against the **live** Gigya site,
 * exercising the full stack for each flow: Compose screen → ViewModel → [com.gigya.android.sample.data.GigyaRepository]
 * → SDK → backend. A green run is a signal that core CDC flows work end to end against
 * the configured site.
 *
 * ### Site configuration
 * The target site is **not** taken from `gigyaSdkConfiguration.json`. [SanityTestRunner]
 * (the custom `AndroidJUnitRunner`) reads `gigya_api_key` / `gigya_api_domain` from the
 * gitignored `secrets.xml` and calls `Gigya.setConfiguration(...)` before the app's
 * `Application.onCreate`, so the SDK initialises against those values and skips the JSON.
 * To point the suite at another site/ENV, change those two strings in `secrets.xml` and
 * re-run — no code change. (Note: `setConfiguration` only carries api key + domain, not the
 * JSON's `account.include` / `extraProfileFields` / `webView` settings.)
 *
 * ### Execution model
 * Tests run in **name order** (`test01_`, `test02_`, …) via [FixMethodOrder]. They form a
 * dependent sequence: `test01` registers an account and stashes its credentials in the
 * [companion object]; later tests read them. The suite is therefore meant to run **whole** —
 * running a later test in isolation fails fast with a "run the full suite" message.
 * Each test still gets a fresh `MainActivity` launch via [composeRule].
 *
 * ### Running
 * ```
 * ./gradlew :example:connectedAndroidTest
 * adb logcat -s SanityTest   # live step-by-step progress
 * ```
 * Requires a connected device/emulator with a populated `secrets.xml`.
 *
 * ### Coverage
 * `test01` register · `test02` login + UID data integrity · `test03` session persistence ·
 * `test04` logout invalidation · `test05` invalid-credentials error path.
 *
 * ### CI
 * GitHub Actions is deferred; CI will use a login-only variant with a fixed pre-existing
 * account. `test01` (register) is device/dev-site only and is excluded from the login-only
 * CI variant.
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class SanityTest {

    /**
     * Grants `POST_NOTIFICATIONS` up front (API 33+) so the runtime permission dialog never
     * steals focus mid-test and pauses the Activity. Order 0 — applied before [composeRule].
     */
    @get:Rule(order = 0)
    val permissionRule: GrantPermissionRule = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        GrantPermissionRule.grant()
    }

    /** Launches [MainActivity] and exposes the Compose test API. A fresh instance per test. */
    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    /** Instrumentation target context — used to read string resources (e.g. `secrets.xml` values). */
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    // region Helpers

    /** Waits until the LoginScreen is shown (Register button present) and asserts it, else times out. */
    private fun waitForLoginScreen() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag(TestTags.BTN_REGISTER))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(TestTags.BTN_REGISTER).assertIsDisplayed()
        log("LoginScreen visible")
    }

    /**
     * Waits for the flow to resolve to either the AccountScreen (UID node) or an error
     * ([TestTags.TEXT_STATUS]), then asserts success. Fails with the SDK error text if the
     * error node appeared first — so a backend failure surfaces as a clear message rather
     * than a generic timeout.
     *
     * @param timeoutMs max wait for the backend round-trip (default 45s).
     */
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

    /** Reads the visible UID string from the AccountScreen node (strips the "UID:" prefix). */
    private fun readUid(): String {
        val node = composeRule.onAllNodes(hasTestTag(TestTags.TEXT_UID)).fetchSemanticsNodes().first()
        return textOf(node).substringAfter("UID:").trim()
    }

    /**
     * Waits for and returns the error status text ([TestTags.TEXT_STATUS]), failing if none
     * appears within [timeoutMs]. Used by negative-path tests that expect an error.
     */
    private fun waitForErrorStatus(timeoutMs: Long = 45_000): String {
        composeRule.waitUntil(timeoutMillis = timeoutMs) {
            composeRule.onAllNodes(hasTestTag(TestTags.TEXT_STATUS))
                .fetchSemanticsNodes().isNotEmpty()
        }
        return textOf(
            composeRule.onAllNodes(hasTestTag(TestTags.TEXT_STATUS)).fetchSemanticsNodes().first()
        )
    }

    /** Extracts the concatenated `Text` semantics of a node (empty string if it carries none). */
    private fun textOf(node: androidx.compose.ui.semantics.SemanticsNode): String =
        node.config
            .getOrElse(androidx.compose.ui.semantics.SemanticsProperties.Text) { emptyList() }
            .joinToString()

    /** Taps Logout and waits for the LoginScreen to return, asserting the session UI was cleared. */
    private fun logout() {
        composeRule.onNodeWithTag(TestTags.BTN_LOGOUT).performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag(TestTags.BTN_REGISTER))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(TestTags.BTN_REGISTER).assertIsDisplayed()
        log("LoginScreen restored after logout")
    }

    /** Emits a step marker to logcat under the [TAG] tag (`adb logcat -s SanityTest`). */
    private fun log(message: String) = Log.d(TAG, message)

    // endregion

    /**
     * Test 01 — Register a new account, verify AccountScreen, logout.
     * Stores the generated credentials in [companion object] for test 02.
     *
     * Preview: enter fresh email+password → tap Register → AccountScreen (UID) → Logout → LoginScreen.
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

        // Emit the created account's UID as a machine-parseable marker so the CI harness can
        // delete this throwaway account after the suite (register runs only in the full suite,
        // so each run would otherwise leave an orphaned user). Captured here — right after a
        // successful register — so cleanup is possible even if a later test fails.
        val uid = readUid()
        testUid = uid
        log("$UID_MARKER$uid")

        logout()
        log("PASS test01_register_createsAccount_andLogsOut")
    }

    /**
     * Test 02 — Login with the account created in test 01, verify AccountScreen with a valid
     * UID (data integrity), logout. Depends on [testEmail]/[testPassword] set by test 01.
     *
     * Preview: enter test01 creds → tap Login → AccountScreen → capture non-blank UID → Logout.
     */
    @Test
    fun test02_login_withRegisteredAccount_andLogsOut() {
        val email = testEmail.ifBlank { context.getString(R.string.test_login_id) }
        val password = testPassword.ifBlank { context.getString(R.string.test_login_password) }

        if (email.isBlank() || password.isBlank()) {
            fail("No credentials: run test01 first, or set test_login_id/test_login_password for login-only CI.")
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
     *
     * Preview: login → capture UID → `recreate()` activity → AccountScreen restored, same UID → Logout.
     */
    @Test
    fun test03_sessionPersists_acrossRelaunch() {
        val email = testEmail.ifBlank { context.getString(R.string.test_login_id) }
        val password = testPassword.ifBlank { context.getString(R.string.test_login_password) }
        if (email.isBlank() || password.isBlank()) {
            fail("No credentials: run test01 first, or set test_login_id/test_login_password for login-only CI.")
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
     *
     * Preview: login → Logout → `recreate()` activity → LoginScreen (no session restored).
     */
    @Test
    fun test04_logoutInvalidatesSession_acrossRelaunch() {
        val email = testEmail.ifBlank { context.getString(R.string.test_login_id) }
        val password = testPassword.ifBlank { context.getString(R.string.test_login_password) }
        if (email.isBlank() || password.isBlank()) {
            fail("No credentials: run test01 first, or set test_login_id/test_login_password for login-only CI.")
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
     *
     * Preview: enter bad email+password → tap Login → error status shown, AccountScreen NOT reached.
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

        /**
         * Log-line prefix marking the UID of the account created by `test01`. The CI harness
         * greps logcat for this exact token to delete the throwaway account after the suite.
         * Keep in sync with the harness (run.sh) parser.
         */
        const val UID_MARKER = "SANITY_UID="

        // Shared state between sequential tests — set by test01, read by later tests.
        /** Email of the account created by `test01`; consumed by `test02`–`test04`. */
        var testEmail: String = ""
        /** Password of the account created by `test01`; consumed by `test02`–`test04`. */
        var testPassword: String = ""
        /** UID captured at login by `test02`; the value `test03` asserts survives a relaunch. */
        var testUid: String = ""
    }
}
