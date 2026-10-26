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
 * ScreenSet sanity E2E test (CXCDC-45188).
 *
 * Covers the hosted-WebView **ScreenSet** auth surface — a different code path
 * from the Compose credentials flow exercised by [SanityTest]. The screenset is
 * launched via [com.gigya.android.sdk.Gigya.showScreenSet] (wired to the
 * "ScreenSet Login" button on the login screen) and rendered by the SDK inside
 * its own plugin fragment as a WebView.
 *
 * ### Why UIAutomator
 * The screenset content lives inside a `WebView`. Compose's test API and
 * Espresso view matchers cannot see into it, so the WebView form is driven with
 * [UiDevice] / UIAutomator, which operates at the device level and can address
 * rendered web content (by text / resource-id / class). The **outcome** is then
 * asserted back on the Compose layer: a successful screenset auth routes to the
 * account screen, so we assert [TestTags.TEXT_UID] is shown.
 *
 * ### Execution model
 * Tests run in **name order** (`test01_`, `test02_`) via [FixMethodOrder] and
 * form a dependent sequence: `test01` drives the screenset **register** form,
 * creating a fresh account and stashing its credentials in the [companion
 * object]; `test02` then drives the screenset **login** form with those same
 * credentials. The suite is self-contained — it needs no pre-existing account
 * and no login secrets, so it is always runnable. Running `test02` in isolation
 * fails fast with a "run the full suite" message.
 *
 * The form field layout and selectors mirror the reference WebdriverIO suite at
 * SAP/gigya-mobile-sdk-tests (`web.form.ts` / `gui.map.ts`): the register form
 * requires email, first name, last name, password, retype-password, plus a Terms
 * consent checkbox; the login form requires only email + password.
 *
 * > Note: each run creates a real account on the target site. Acceptable for a
 * > dev/test site; a CI variant should clean these up (see [SanityTest]'s
 * > `SANITY_UID` marker approach).
 *
 * ### Site configuration
 * Requires the "Default-RegistrationLogin" screenset to be configured on the
 * target site. Site is set from `secrets.xml` by [SanityTestRunner] (same as
 * [SanityTest]).
 *
 * ### Running
 * ```
 * ./gradlew :example:connectedAndroidTest
 * adb logcat -s ScreenSetSanityTest
 * ```
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ScreenSetSanityTest {

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

    /**
     * Ensures each test starts at the LoginScreen. If a previous run left an active
     * session (app launches directly to AccountScreen), tap Logout first.
     */
    @Before
    fun ensureLoggedOut() {
        // Wait briefly for the app to settle on either screen.
        composeRule.waitUntil(timeoutMillis = 15_000) {
            val onLogin = composeRule.onAllNodes(hasTestTag(TestTags.BTN_SHOW_SCREENSET))
                .fetchSemanticsNodes().isNotEmpty()
            val onAccount = composeRule.onAllNodes(hasTestTag(TestTags.BTN_LOGOUT))
                .fetchSemanticsNodes().isNotEmpty()
            onLogin || onAccount
        }
        // If we landed on AccountScreen (active session), log out.
        val logoutNodes = composeRule.onAllNodes(hasTestTag(TestTags.BTN_LOGOUT)).fetchSemanticsNodes()
        if (logoutNodes.isNotEmpty()) {
            log("Active session detected — logging out before test")
            composeRule.onNodeWithTag(TestTags.BTN_LOGOUT).performScrollTo().performClick()
            composeRule.waitUntil(timeoutMillis = 10_000) {
                composeRule.onAllNodes(hasTestTag(TestTags.BTN_SHOW_SCREENSET))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            log("Logged out — now at LoginScreen")
        }
    }


    /**
     * Test 01 — Register via the screenset.
     *
     * Opens the login screenset, drives the WebView **register** form via
     * UIAutomator (creating a fresh account), and asserts the flow lands on the
     * account screen with a UID. Stashes the created credentials in the
     * [companion object] for `test02` to log in with.
     */
    @Test
    fun test01_screenSetRegister_createsAccount() {
        val email = "${context.getString(R.string.test_email_prefix)}_ss_${System.currentTimeMillis()}@gigya-test.com"
        val password = "Test1234!"

        // Persist for test02 (login).
        testEmail = email
        testPassword = password

        log("START test01_screenSetRegister | email=$email")

        // Wait for the Compose login screen, then open the screenset.
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag(TestTags.BTN_SHOW_SCREENSET))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(TestTags.BTN_SHOW_SCREENSET).performScrollTo().performClick()
        log("ScreenSet button tapped — waiting for WebView...")

        // The screenset is now a WebView over the activity. Drive it via UIAutomator.
        fillScreenSetRegisterForm(email, password)

        // HostActivity (SDK WebView host) takes focus during the screenset flow, moving
        // MainActivity to STOPPED. composeRule is bound to MainActivity, so querying it
        // while HostActivity is visible throws "No compose hierarchies found". Wait for
        // the WebView to dismiss (HostActivity gone), then retry until MainActivity's
        // Compose tree is registered again.
        waitForHostActivityDismissed()
        waitForComposeNodeWithTag(TestTags.TEXT_UID, timeoutMillis = 30_000)
        composeRule.onNodeWithTag(TestTags.TEXT_UID).assertIsDisplayed()

        // Emit the created account's UID as a machine-parseable marker so the CI harness
        // can delete this throwaway account after the suite (mirrors SanityTest's approach).
        val uid = readUid()
        testUid = uid
        log("$UID_MARKER$uid")

        log("PASS test01_screenSetRegister — AccountScreen shown with UID")
    }

    /**
     * Test 02 — Login via the screenset.
     *
     * Opens the login screenset, drives the WebView **login** form via UIAutomator
     * using the credentials `test01` created, and asserts the flow lands on the
     * account screen with a UID. `@Before` has already logged out the session
     * `test01` left active, so the app is at the login screen.
     *
     * Depends on `test01` — fails fast if run in isolation (no stashed credentials).
     */
    @Test
    fun test02_screenSetLogin_withCreatedAccount() {
        val email = testEmail
        val password = testPassword
        if (email.isBlank() || password.isBlank()) {
            fail("test02 depends on test01 — run the full suite so credentials are created")
        }

        log("START test02_screenSetLogin | email=$email")

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag(TestTags.BTN_SHOW_SCREENSET))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(TestTags.BTN_SHOW_SCREENSET).performScrollTo().performClick()
        log("ScreenSet button tapped — waiting for WebView...")

        fillScreenSetLoginForm(email, password)

        waitForHostActivityDismissed()
        waitForComposeNodeWithTag(TestTags.TEXT_UID, timeoutMillis = 30_000)
        composeRule.onNodeWithTag(TestTags.TEXT_UID).assertIsDisplayed()
        log("PASS test02_screenSetLogin — AccountScreen shown with UID")
    }

    /**
     * Fills the screenset **registration** form inside the WebView and submits it.
     *
     * The "Default-RegistrationLogin" screenset opens on the login sub-screen; a
     * link switches to registration. Gigya screensets render standard HTML inputs,
     * addressed here by UIAutomator EditText index.
     *
     * The site's register form requires five text fields plus a Terms consent
     * checkbox (see SAP/gigya-mobile-sdk-tests `web.form.ts`): email, password,
     * retype-password, first name, last name. Leaving first/last name empty — or
     * the Terms box unchecked — fails client-side validation silently, so the
     * SDK never fires `accounts.register` and the flow hangs on the WebView.
     *
     * Field order in the accessibility tree matches the form DOM: [0] email,
     * [1] first name, [2] last name, [3] password, [4] retype-password (confirmed
     * from the screenset's `beforeValidation` formData). Email is filled first
     * with a pause so the async `isAvailableLoginID` check settles before the
     * rest of the form is completed (mirrors the reference flow). Each field is
     * clicked to focus before setting text, so the WebView's JS validators fire.
     */
    private fun fillScreenSetRegisterForm(email: String, password: String) {
        waitForWebViewAndIdle("post-screenset-render snapshot")

        log("WebView detected — switching to register sub-screen")

        // The Default-RegistrationLogin screenset opens on the LOGIN sub-screen.
        // The switch-to-register control is a link labelled "Don't have an account yet?".
        val toRegister = device.wait(
            Until.findObject(By.textContains("Don't have an account")),
            10_000,
        )
        if (toRegister == null) {
            dumpHierarchy("register toggle link not found")
            fail("Could not find the 'Don't have an account yet?' link")
        }
        toRegister!!.click()
        log("Tapped switch-to-register link")
        device.waitForIdle()
        Thread.sleep(2_000)
        dumpHierarchy("register sub-screen snapshot")

        log("Locating register form fields")

        // Email first, then pause so the async availability check settles.
        fillField(0, email, "Email")
        device.waitForIdle()
        Thread.sleep(3_000)

        fillField(1, "Sanity", "First name")
        fillField(2, "Tester", "Last name")
        fillField(3, password, "Password")
        fillField(4, password, "Retype password")

        logFieldValues()
        tapSubmit("register form")
    }

    /**
     * Fills the screenset **login** form inside the WebView and submits it.
     *
     * The "Default-RegistrationLogin" screenset opens on the login sub-screen
     * directly — no navigation needed. Fills email + password and submits.
     *
     * After login, the site's screenset may route to a profile-completion
     * interstitial (`gigya-update-profile-screen`, form `data-on-success-screen=
     * "_finish"`) before it fires `onLogin` and closes — this happens for the
     * freshly-registered, not-yet-verified account created by `test01`. The
     * screenset will not auto-finish; it waits for that screen to be submitted,
     * so [handleProfileCompletionIfPresent] submits it to let the flow complete.
     */
    private fun fillScreenSetLoginForm(email: String, password: String) {
        waitForWebViewAndIdle("post-screenset-render snapshot (login)")

        log("WebView detected — filling login form")
        dumpHierarchy("login sub-screen snapshot")

        fillField(0, email, "Email")
        fillField(1, password, "Password")

        logFieldValues()
        tapSubmit("login form")

        dismissScreenSetAfterLogin()
    }

    /**
     * After a successful login, dismisses the screenset so the app returns to the
     * account screen.
     *
     * Login itself establishes the session immediately (`accounts.login` → active
     * session). However, for the freshly-registered, not-yet-verified account the
     * site's screenset does not close on login — it routes to a post-login
     * "Profile Details" screen (`gigya-update-profile-screen`) and stays open. The
     * session is already valid at that point, so the correct action is simply to
     * dismiss the screenset: press Back to close the SDK's HostActivity. When it
     * closes, MainActivity resumes, observes the active session, and shows the
     * account screen.
     *
     * We first confirm login succeeded by waiting for the post-login profile
     * screen to appear (it only loads once authenticated), then press Back. If the
     * WebView dismisses on its own first (clean finish), this is a no-op.
     */
    private fun dismissScreenSetAfterLogin() {
        // Wait for evidence that login succeeded: either the WebView already
        // dismissed (clean finish) or the post-login profile screen rendered.
        val deadline = System.currentTimeMillis() + 15_000
        var loginConfirmed = false
        while (System.currentTimeMillis() < deadline) {
            if (device.findObject(By.clazz("android.webkit.WebView")) == null) {
                log("Login finished cleanly — WebView already dismissed")
                return
            }
            // The post-login profile screen is proof the session is active.
            val profileScreen = device.findObject(By.res("gigya-update-profile-screen"))
                ?: device.findObject(By.res("gigya-textbox-firstName"))
                ?: device.findObject(By.clazz("android.widget.Button").text("Save"))
            if (profileScreen != null) {
                loginConfirmed = true
                break
            }
            Thread.sleep(1_000)
        }

        if (!loginConfirmed) {
            log("Post-login profile screen did not appear; attempting back-dismiss anyway")
        } else {
            log("Login confirmed (post-login profile screen shown) — dismissing screenset")
        }

        // Session is active — just close the screenset to land on the account screen.
        device.pressBack()
        device.waitForIdle()
        Thread.sleep(1_000)
    }

    // region Shared UIAutomator helpers

    /**
     * Waits until the SDK's HostActivity WebView is visible and the form has had
     * time to paint (network fetch + JS render). Fails the test if the WebView
     * never appears.
     */
    private fun waitForWebViewAndIdle(dumpReason: String) {
        val webViewReady = device.wait(Until.hasObject(By.clazz("android.webkit.WebView")), 20_000)
        log("WebView present=$webViewReady")
        device.waitForIdle()
        Thread.sleep(6_000)
        dumpHierarchy(dumpReason)
        if (webViewReady == null || !webViewReady) {
            fail("ScreenSet WebView did not appear")
        }
    }

    /** Fills a single WebView EditText field by index, clicking first so JS handlers fire. */
    private fun fillField(index: Int, value: String, label: String) {
        val current = device.wait(Until.findObjects(By.clazz("android.widget.EditText")), 5_000)
        if (current == null || current.size <= index) {
            dumpHierarchy("field #$index ($label) not present")
            fail("Could not locate the $label field on the screenset form")
        }
        val field = current!![index]
        field.click()
        device.waitForIdle()
        field.text = value
        device.waitForIdle()
        log("$label entered")
    }

    /** Logs the current text of every EditText for selector debugging. */
    private fun logFieldValues() {
        device.findObjects(By.clazz("android.widget.EditText")).forEachIndexed { i, f ->
            log("  field[$i] now = '${f.text}'")
        }
    }

    /**
     * Taps the form submit button.
     *
     * Dismisses the soft keyboard first (it blocks WebView content from the
     * accessibility tree while open), then scrolls the page down to bring the
     * submit element into view, checks any required Terms checkbox, and taps
     * the submit button.
     *
     * Register sub-screen: checks `id='gigya-checkbox-terms'` if unchecked,
     * then taps `android.widget.Button text='Submit'`. The outer container
     * `id='register-site-login'` is a View wrapper — clicking it does not
     * trigger the JS submit handler.
     * Login sub-screen: the submit button is `android.widget.Button text='submit'`
     * (lowercase, CSS-rendered).
     */
    private fun tapSubmit(formLabel: String) {
        // Dismiss the soft keyboard so WebView content is accessible.
        device.pressBack()
        device.waitForIdle()
        Thread.sleep(300)

        // Scroll down so the submit button (below the form fields) enters the viewport.
        device.swipe(540, 1400, 540, 700, 10)
        device.waitForIdle()
        Thread.sleep(800)
        dumpHierarchy("post-scroll snapshot before submit ($formLabel)")

        // Check the Terms of Use checkbox if it's required and unchecked.
        val termsCheckbox = device.findObject(By.res("gigya-checkbox-terms"))
        if (termsCheckbox != null && !termsCheckbox.isChecked) {
            log("Terms checkbox found and unchecked — checking it")
            termsCheckbox.click()
            device.waitForIdle()
            Thread.sleep(300)
        }

        val submit = device.findObject(By.clazz("android.widget.Button").text("Submit"))
            ?: device.findObject(By.clazz("android.widget.Button").text("submit"))
            ?: device.findObject(By.clazz("android.widget.Button").textContains("Register"))
            ?: device.findObject(By.clazz("android.widget.Button").textContains("Submit"))
            ?: device.findObject(By.res("register-site-login"))
            ?: device.findObject(By.res("gigya-login-submit"))
            ?: device.findObject(By.clazz("android.view.View").text("REGISTER"))
            ?: device.findObject(By.clazz("android.view.View").text("LOGIN"))
        if (submit == null) {
            dumpHierarchy("submit button not found on $formLabel")
            fail("Could not locate the submit button on the $formLabel")
        }
        submit!!.click()
        log("Submit tapped — waiting for session...")
        device.waitForIdle()
        Thread.sleep(3_000)
        dumpHierarchy("post-submit snapshot (look for validation errors)")
    }

    /**
     * Waits until the SDK HostActivity (WebView host) is no longer in the
     * foreground — i.e., the WebView disappears from the view hierarchy.
     * Called after submit so we don't query composeRule while HostActivity is
     * still visible (MainActivity is STOPPED in that window).
     */
    private fun waitForHostActivityDismissed(timeoutMillis: Long = 30_000) {
        val dismissed = device.wait(Until.gone(By.clazz("android.webkit.WebView")), timeoutMillis)
        log("HostActivity dismissed (WebView gone)=$dismissed")
        device.waitForIdle()
        // Brief pause for MainActivity to resume and Compose to recompose.
        Thread.sleep(1_000)
    }

    /**
     * Polls [composeRule.waitUntil] for a node with [tag] until it appears or
     * [timeoutMillis] expires. Re-tries on [IllegalStateException] ("No compose
     * hierarchies") which occurs during the window between HostActivity dismiss
     * and MainActivity fully resuming.
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
                // waitUntil timed out — keep polling if we have budget left
                if (System.currentTimeMillis() >= deadline) throw e
                Thread.sleep(500)
            }
        }
        fail("Timed out waiting for Compose node with tag '$tag' after ${timeoutMillis}ms")
    }

    // endregion

    /** Reads the visible UID string from the AccountScreen node (strips the "UID:" prefix). */
    private fun readUid(): String {
        val node = composeRule.onAllNodes(hasTestTag(TestTags.TEXT_UID)).fetchSemanticsNodes().first()
        return node.config
            .getOrElse(androidx.compose.ui.semantics.SemanticsProperties.Text) { emptyList() }
            .joinToString()
            .substringAfter("UID:").trim()
    }

    /** Dumps every addressable node (class, text, resource-id, desc) to logcat for selector debugging. */
    private fun dumpHierarchy(reason: String) {
        log("HIERARCHY DUMP ($reason):")
        runCatching {
            device.findObjects(By.clazz(java.util.regex.Pattern.compile(".*"))).forEachIndexed { i, o ->
                log("  [$i] class=${o.className} text='${o.text}' id='${o.resourceName}' desc='${o.contentDescription}'")
            }
        }.onFailure { log("  dump error: ${it.message}") }
    }

    companion object {
        private const val TAG = "ScreenSetSanityTest"

        /**
         * Log-line prefix marking the UID of the account created by `test01`. The CI harness
         * greps logcat for this exact token to delete the throwaway account after the suite.
         * Keep in sync with the harness (run.sh) parser.
         */
        const val UID_MARKER = "SANITY_UID="

        // Shared state between the sequential tests — set by test01, read by test02.
        /** Email of the account created by `test01`; consumed by `test02`. */
        var testEmail: String = ""
        /** Password of the account created by `test01`; consumed by `test02`. */
        var testPassword: String = ""
        /** UID captured after register by `test01`; emitted as SANITY_UID for CI cleanup. */
        var testUid: String = ""
    }
}
