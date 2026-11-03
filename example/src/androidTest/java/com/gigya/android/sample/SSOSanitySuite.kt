package com.gigya.android.sample

import android.Manifest
import android.os.Build
import android.util.Log
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.gigya.android.sample.ui.MainActivity
import com.gigya.android.sample.ui.common.TestTags
import org.junit.Assert.fail
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 * SSO sanity E2E suite — exercises the mobile-SSO surface via Gigya.sso().
 *
 * The SDK opens the login page in a Chrome Custom Tab (a separate com.android.chrome
 * process), which is a completely different code path from both SanityTest (native
 * Compose) and ScreenSetSanityTest (in-app WebView). UIAutomator is used to drive
 * the Chrome CCT form; Compose assertions verify the outcome once Chrome dismisses.
 *
 * test01: register via the SSO page (switches to the register sub-screen).
 * test02: login via the SSO page using the account test01 created.
 *
 * SANITY_UID is emitted in test01 so the CI harness can delete the throwaway account.
 * Never press Back during the flow — GigyaSSOLoginActivity.onResume() calls onCancelled()
 * if it detects a pause, which cancels SSO and returns an error instead of a result.
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class SSOSanitySuite {

    @get:Rule(order = 0)
    val permissionRule: GrantPermissionRule = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        GrantPermissionRule.grant()
    }

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val device: UiDevice get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private fun log(message: String) = Log.d(TAG, message)

    @Before
    fun ensureLoggedOut() {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            val onLogin = composeRule.onAllNodes(hasTestTag(TestTags.BTN_SSO))
                .fetchSemanticsNodes().isNotEmpty()
            val onAccount = composeRule.onAllNodes(hasTestTag(TestTags.BTN_LOGOUT))
                .fetchSemanticsNodes().isNotEmpty()
            onLogin || onAccount
        }
        val logoutNodes = composeRule.onAllNodes(hasTestTag(TestTags.BTN_LOGOUT)).fetchSemanticsNodes()
        if (logoutNodes.isNotEmpty()) {
            log("Active session detected — logging out before test")
            composeRule.onNodeWithTag(TestTags.BTN_LOGOUT).performScrollTo().performClick()
            composeRule.waitUntil(timeoutMillis = 10_000) {
                composeRule.onAllNodes(hasTestTag(TestTags.BTN_SSO))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            log("Logged out — now at LoginScreen")
        }
    }

    @Test
    fun test01_ssoRegister_createsAccount() {
        val email = "${context.getString(R.string.test_email_prefix)}_sso_${System.currentTimeMillis()}@gigya-test.com"
        val password = "Test1234!"
        testEmail = email
        testPassword = password

        log("START test01_ssoRegister | email=$email")

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag(TestTags.BTN_SSO))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(TestTags.BTN_SSO).performScrollTo().performClick()
        log("SSO button tapped — waiting for Chrome Custom Tab...")

        fillSSORegisterForm(email, password)

        waitForCustomTabDismissed()
        waitForComposeNodeWithTag(TestTags.TEXT_UID, timeoutMillis = 30_000)
        composeRule.onNodeWithTag(TestTags.TEXT_UID).assertIsDisplayed()

        val uid = readUid()
        testUid = uid
        log("$UID_MARKER$uid")

        log("PASS test01_ssoRegister — AccountScreen shown with UID=$uid")
    }

    @Test
    fun test02_ssoLogin_withCreatedAccount() {
        val email = testEmail
        val password = testPassword
        if (email.isBlank() || password.isBlank()) {
            fail("test02 depends on test01 — run the full suite so credentials are created")
        }

        log("START test02_ssoLogin | email=$email")

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag(TestTags.BTN_SSO))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(TestTags.BTN_SSO).performScrollTo().performClick()
        log("SSO button tapped — waiting for Chrome Custom Tab...")

        fillSSOLoginForm(email, password)

        waitForCustomTabDismissed()
        waitForComposeNodeWithTag(TestTags.TEXT_UID, timeoutMillis = 30_000)
        composeRule.onNodeWithTag(TestTags.TEXT_UID).assertIsDisplayed()
        log("PASS test02_ssoLogin — AccountScreen shown with UID")
    }

    // region CCT form helpers

    private fun fillSSORegisterForm(email: String, password: String) {
        waitForCustomTabAndIdle("post-CCT-render snapshot (login sub-screen)")

        log("Chrome Custom Tab detected — switching to register sub-screen")

        // The SSO page is the same Default-RegistrationLogin screenset as ScreenSet,
        // but served in a Chrome Custom Tab. Switch to register with the same link text.
        val toRegister = device.wait(
            Until.findObject(By.textContains("Don't have an account")),
            10_000,
        )
        if (toRegister == null) {
            dumpHierarchy("register toggle link not found in CCT")
            fail("Could not find the switch-to-register link in the SSO Chrome Custom Tab")
        }
        toRegister!!.click()
        log("Tapped switch-to-register link")
        device.waitForIdle()
        Thread.sleep(1_500)
        dumpHierarchy("register sub-screen in CCT")

        // Fill the register form. Field order: [0] email, [1] firstName, [2] lastName,
        // [3] password, [4] retype-password.
        //
        // Fetch ALL 5 field objects BEFORE filling any of them. Clicking a field raises the
        // soft keyboard which pushes the bottom fields out of the accessibility viewport —
        // re-fetching after each fill only sees what's currently on screen. Pre-fetching the
        // objects keeps the references valid even after they scroll off screen.
        val allFields = device.wait(Until.findObjects(By.clazz("android.widget.EditText")), 8_000)
        if (allFields == null || allFields.size < 5) {
            dumpHierarchy("register form: expected 5 fields, found ${allFields?.size ?: 0}")
            fail("Expected 5 EditText fields on register form, got ${allFields?.size ?: 0}")
        }
        log("Register form: ${allFields!!.size} fields found — filling in order")

        // [0] email — pause after to let async loginID availability check settle
        allFields[0].click(); device.waitForIdle(); allFields[0].text = email
        log("Email entered")
        device.waitForIdle()
        Thread.sleep(1_500)

        allFields[1].click(); device.waitForIdle(); allFields[1].text = "Sanity"; log("First name entered")
        device.waitForIdle()
        allFields[2].click(); device.waitForIdle(); allFields[2].text = "Tester"; log("Last name entered")
        device.waitForIdle()
        allFields[3].click(); device.waitForIdle(); allFields[3].text = password; log("Password entered")
        device.waitForIdle()
        allFields[4].click(); device.waitForIdle(); allFields[4].text = password; log("Retype password entered")
        device.waitForIdle()

        logFieldValues()

        // Check Terms checkbox if present and unchecked.
        val termsCheckbox = device.findObject(By.res("gigya-checkbox-terms"))
        if (termsCheckbox != null && !termsCheckbox.isChecked) {
            log("Terms checkbox found and unchecked — checking it")
            termsCheckbox.click()
            device.waitForIdle()
        }

        // Submit via ENTER keycode — matches the reference WebdriverIO suite (pressKeyCode 66).
        // Do NOT press Back (would cancel SSO via GigyaSSOLoginActivity.onResume).
        log("Submitting register form via ENTER keycode")
        device.pressKeyCode(66)
        device.waitForIdle()
        Thread.sleep(2_000)
        dumpHierarchy("post-submit register snapshot")
    }

    private fun fillSSOLoginForm(email: String, password: String) {
        waitForCustomTabAndIdle("post-CCT-render snapshot (login sub-screen for login)")

        log("Chrome Custom Tab detected — filling login form")
        dumpHierarchy("login sub-screen in CCT")

        // [0] loginID/email, [1] password
        cctFillField(0, email, "Email")
        cctFillField(1, password, "Password")

        logFieldValues()

        log("Submitting login form via ENTER keycode")
        device.pressKeyCode(66)
        device.waitForIdle()
        Thread.sleep(2_000)
        dumpHierarchy("post-submit login snapshot")
    }

    /**
     * Waits for the Chrome Custom Tab to become foreground and the page to render.
     * Unlike ScreenSet which uses android.webkit.WebView, CCT runs in the com.android.chrome
     * package — UIAutomator can cross the process boundary to inspect its accessibility tree.
     *
     * On a fresh emulator Chrome shows its First-Run Experience (FRE) sign-in dialog before
     * the SSO page renders. We dismiss it by tapping "Stay signed out" (no Google account
     * needed) and wait for the Gigya page to appear afterward.
     */
    private fun waitForCustomTabAndIdle(dumpReason: String) {
        val present = device.wait(Until.hasObject(By.pkg("com.android.chrome")), 20_000)
        log("Chrome Custom Tab present=$present")
        if (present == null || !present) {
            fail("Chrome Custom Tab did not appear within 20s")
        }
        device.waitForIdle()
        Thread.sleep(1_000)

        // Dismiss Chrome FRE if it appears (first launch on a fresh emulator/device).
        val staySignedOut = device.findObject(By.res("com.android.chrome:id/signin_fre_dismiss_button"))
            ?: device.findObject(By.text("Stay signed out"))
            ?: device.findObject(By.textContains("Stay signed out"))
        if (staySignedOut != null) {
            log("Chrome FRE detected — tapping 'Stay signed out' to dismiss")
            staySignedOut.click()
            device.waitForIdle()
            Thread.sleep(1_000)
            log("Chrome FRE dismissed")
        }

        // Wait for the Gigya SSO page to render (EditText fields appear).
        log("Waiting for Gigya SSO page to render...")
        val pageReady = device.wait(Until.hasObject(By.clazz("android.widget.EditText")), 15_000)
        log("SSO page fields present=$pageReady")
        device.waitForIdle()
        Thread.sleep(1_000)
        dumpHierarchy(dumpReason)
    }

    /**
     * Waits until the Chrome Custom Tab (com.android.chrome) is no longer foreground.
     * This is the CCT analog of waitForHostActivityDismissed() in ScreenSetSanityTest.
     * Do NOT wait on WebView disappearance — the CCT is a separate Chrome process, not a WebView.
     */
    private fun waitForCustomTabDismissed(timeoutMillis: Long = 30_000) {
        val dismissed = device.wait(Until.gone(By.pkg("com.android.chrome")), timeoutMillis)
        log("Chrome Custom Tab dismissed=$dismissed")
        device.waitForIdle()
    }

    /**
     * Fills a single EditText in the Chrome Custom Tab by index.
     * CCT fields are accessible via UIAutomator exactly like WebView fields.
     */
    private fun cctFillField(index: Int, value: String, label: String) {
        val fields = device.wait(Until.findObjects(By.clazz("android.widget.EditText")), 5_000)
        if (fields == null || fields.size <= index) {
            dumpHierarchy("CCT field #$index ($label) not found")
            fail("Could not locate the CCT $label field at index $index")
        }
        val field = fields!![index]
        field.click()
        device.waitForIdle()
        field.text = value
        device.waitForIdle()
        log("CCT $label entered at index=$index")
    }

    private fun logFieldValues() {
        device.findObjects(By.clazz("android.widget.EditText")).forEachIndexed { i, f ->
            log("  field[$i] now = '${f.text}'")
        }
    }

    // endregion

    // region Compose helpers (same as ScreenSetSanityTest)

    /**
     * Polls for a Compose node with [tag]. Tolerates IllegalStateException ("No compose
     * hierarchies found") which fires while Chrome is still foreground (MainActivity STOPPED).
     */
    private fun waitForComposeNodeWithTag(tag: String, timeoutMillis: Long) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            try {
                composeRule.waitUntil(timeoutMillis = minOf(5_000L, deadline - System.currentTimeMillis())) {
                    composeRule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
                }
                return
            } catch (e: IllegalStateException) {
                log("Compose not ready yet (MainActivity still resuming): ${e.message}")
                Thread.sleep(500)
            } catch (e: AssertionError) {
                if (System.currentTimeMillis() >= deadline) throw e
                Thread.sleep(500)
            }
        }
        fail("Timed out waiting for Compose node with tag '$tag' after ${timeoutMillis}ms")
    }

    private fun readUid(): String {
        val node = composeRule.onAllNodes(hasTestTag(TestTags.TEXT_UID)).fetchSemanticsNodes().first()
        return node.config
            .getOrElse(androidx.compose.ui.semantics.SemanticsProperties.Text) { emptyList() }
            .joinToString()
            .substringAfter("UID:").trim()
    }

    private fun dumpHierarchy(reason: String) {
        log("HIERARCHY DUMP ($reason):")
        runCatching {
            device.findObjects(By.clazz(java.util.regex.Pattern.compile(".*"))).forEachIndexed { i, o ->
                log("  [$i] class=${o.className} text='${o.text}' id='${o.resourceName}' desc='${o.contentDescription}'")
            }
        }.onFailure { log("  dump error: ${it.message}") }
    }

    // endregion

    companion object {
        private const val TAG = "SSOSanitySuite"
        const val UID_MARKER = "SANITY_UID="
        var testEmail: String = ""
        var testPassword: String = ""
        var testUid: String = ""
    }
}
