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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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
 * asserted back on the Compose layer: a successful screenset register routes to
 * the account screen, so we assert [TestTags.TEXT_UID] is shown.
 *
 * ### Flow: register (not login)
 * The test drives the screenset's **registration** path, creating a fresh
 * account each run (unique email, password "test"). This needs no pre-existing
 * account and no secrets — the suite is always runnable. The "Default-
 * RegistrationLogin" screenset renders a combined form; the test switches to the
 * register sub-screen if needed, then fills email + password and submits.
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
     * Launches the login screenset, drives the WebView **register** form via
     * UIAutomator, and asserts the flow lands on the account screen with a UID.
     */
    @Test
    fun screenSetRegister_createsAccount_andSession() {
        val email = "${context.getString(R.string.test_email_prefix)}_ss_${System.currentTimeMillis()}@gigya-test.com"
        val password = "test"

        log("START screenSetRegister | email=$email")

        // Wait for the Compose login screen, then open the screenset.
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag(TestTags.BTN_SHOW_SCREENSET))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(TestTags.BTN_SHOW_SCREENSET).performScrollTo().performClick()
        log("ScreenSet button tapped — waiting for WebView...")

        // The screenset is now a WebView over the activity. Drive it via UIAutomator.
        fillScreenSetRegisterForm(email, password)

        // Outcome assertion back on the Compose layer: success routes to AccountScreen.
        composeRule.waitUntil(timeoutMillis = 60_000) {
            composeRule.onAllNodes(hasTestTag(TestTags.TEXT_UID))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(TestTags.TEXT_UID).assertIsDisplayed()
        log("PASS screenSetRegister — AccountScreen shown with UID")
    }

    /**
     * Fills the screenset **registration** form inside the WebView and submits it.
     *
     * The "Default-RegistrationLogin" screenset opens on the login sub-screen; a
     * link/tab switches to registration. Gigya screensets render standard HTML
     * inputs, addressed here by UIAutomator. Selectors are best-effort; if the
     * form cannot be found the current view hierarchy is dumped to logcat to make
     * the actual selectors visible for adjustment.
     */
    private fun fillScreenSetRegisterForm(email: String, password: String) {
        // The screenset launches a separate HostActivity + WebView and then fetches
        // accounts.getScreenSets over the network before the form renders. Wait for
        // the WebView to attach, then give the HTML time to paint, then dump.
        val webViewReady = device.wait(Until.hasObject(By.clazz("android.webkit.WebView")), 20_000)
        log("WebView present=$webViewReady")
        device.waitForIdle()
        Thread.sleep(6_000)
        dumpHierarchy("post-screenset-render snapshot")

        if (webViewReady == null || !webViewReady) {
            fail("ScreenSet WebView did not appear")
        }
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

        // Register form fields (Default-RegistrationLogin register sub-screen):
        //   [0] email, [1] password, [2] confirm-password. Checkboxes (Terms) are
        //   not required on this site. Any further EditTexts are left untouched.
        //
        // Each field is clicked (to focus) before setting text, so the WebView's
        // JS input/validation handlers fire — otherwise the form's client-side
        // validation treats the fields as empty and the WebBridge never fires the
        // register request. Fields are re-fetched per index to avoid stale refs.
        fun fillField(index: Int, value: String, label: String) {
            val current = device.wait(Until.findObjects(By.clazz("android.widget.EditText")), 5_000)
            if (current == null || current.size <= index) {
                dumpHierarchy("field #$index ($label) not present")
                fail("Could not locate the $label field on the register form")
            }
            val field = current!![index]
            field.click()
            device.waitForIdle()
            field.text = value
            device.waitForIdle()
            log("$label entered")
        }

        fillField(0, email, "Email")
        fillField(1, password, "Password")
        fillField(2, password, "Confirm password")

        // Verify what actually landed in each input (WebView text commit check).
        device.findObjects(By.clazz("android.widget.EditText")).forEachIndexed { i, f ->
            log("  field[$i] now = '${f.text}'")
        }

        // Submit the register form. A successful register fires the SDK's
        // GigyaPluginCallback.onLogin (bridged to LoginState.Success) and the
        // screenset dismisses — the outcome is asserted on the Compose layer below.
        val submit = device.findObject(By.clazz("android.widget.Button").text("Submit"))
            ?: device.findObject(By.clazz("android.widget.Button").textContains("Register"))
            ?: device.findObject(By.clazz("android.widget.Button").textContains("Submit"))
        if (submit == null) {
            dumpHierarchy("submit button not found on register form")
            fail("Could not locate the submit button on the register form")
        }
        submit!!.click()
        log("Submit tapped — waiting for session...")
        device.waitForIdle()
        Thread.sleep(3_000)
        dumpHierarchy("post-submit snapshot (look for validation errors)")
    }

    /** Dumps every addressable node (class, text, resource-id, desc) to logcat for selector debugging. */
    private fun dumpHierarchy(reason: String) {
        log("HIERARCHY DUMP ($reason):")
        runCatching {
            val all = device.findObjects(By.pkg(context.packageName).depth(0))
            // Fallback: enumerate all nodes regardless of package.
            device.findObjects(By.clazz(java.util.regex.Pattern.compile(".*"))).forEachIndexed { i, o ->
                log("  [$i] class=${o.className} text='${o.text}' id='${o.resourceName}' desc='${o.contentDescription}'")
            }
        }.onFailure { log("  dump error: ${it.message}") }
    }

    companion object {
        private const val TAG = "ScreenSetSanityTest"
    }
}
