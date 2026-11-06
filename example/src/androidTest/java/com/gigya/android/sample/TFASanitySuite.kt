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
import com.gigya.android.sdk.Gigya
import com.gigya.android.sample.ui.MainActivity
import com.gigya.android.sample.ui.common.TestTags
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 * TFA sanity E2E suite — exercises phone Two-Factor Authentication via the
 * Default-RegistrationLogin ScreenSet on a TFA-enabled test site.
 *
 * All credentials (TFA API key, extension URL, extension token) are injected
 * at build time via generateSecrets → secrets.xml and read by SanityTestRunner.Secrets
 * before the test starts. No secrets live in source.
 *
 * test01: register fresh account → TFA phone registration prompt → enter random US phone →
 *         send code → fetch code from extension store → verify → AccountScreen → emit SANITY_UID.
 */
@RunWith(AndroidJUnit4::class)
class TFASanitySuite {

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

    private fun log(msg: String) = Log.d(TAG, msg)

    @Before
    fun setup() {
        val tfaApiKey = SanityTestRunner.Secrets.tfaApiKey
        val tfaApiDomain = SanityTestRunner.Secrets.tfaApiDomain
        if (tfaApiKey.isBlank()) fail("tfa_api_key is missing from secrets.xml — set TFA_API_KEY env var")

        // Switch the SDK to the TFA site.
        // Must use getInstance().init() — setConfiguration() only applies before the first
        // getInstance() call and has no effect once the SDK singleton is already alive.
        Gigya.getInstance().init(tfaApiKey, tfaApiDomain)
        log("SDK re-initialized: apiKey=${tfaApiKey.take(8)}… domain=$tfaApiDomain")

        // Wait for the app to settle on either LoginScreen or AccountScreen.
        composeRule.waitUntil(timeoutMillis = 20_000) {
            val onLogin = composeRule.onAllNodes(hasTestTag(TestTags.BTN_SHOW_SCREENSET))
                .fetchSemanticsNodes().isNotEmpty()
            val onAccount = composeRule.onAllNodes(hasTestTag(TestTags.BTN_LOGOUT))
                .fetchSemanticsNodes().isNotEmpty()
            onLogin || onAccount
        }

        // If the UI still shows AccountScreen (stale session state), tap logout.
        val logoutNodes = composeRule.onAllNodes(hasTestTag(TestTags.BTN_LOGOUT)).fetchSemanticsNodes()
        if (logoutNodes.isNotEmpty()) {
            log("AccountScreen visible — tapping logout to return to LoginScreen")
            composeRule.onNodeWithTag(TestTags.BTN_LOGOUT).performScrollTo().performClick()
            composeRule.waitUntil(timeoutMillis = 10_000) {
                composeRule.onAllNodes(hasTestTag(TestTags.BTN_SHOW_SCREENSET))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            log("Logout complete — now at LoginScreen")
        }

        log("Setup complete — LoginScreen ready")
    }

    @Test
    fun test01_tfaRegister_withPhoneTFA() {
        // Generate a fresh random US number each run — no shared/static phone number needed.
        // The extension intercepts the code so no real SMS is sent.
        val testPhone = generateRandomUsPhone()

        val email = "${context.getString(R.string.test_email_prefix)}_tfa_${System.currentTimeMillis()}@gigya-test.com"
        val password = "Test1234!"

        log("START test01_tfaRegister | email=$email phone=$testPhone")

        // Open the ScreenSet.
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag(TestTags.BTN_SHOW_SCREENSET))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(TestTags.BTN_SHOW_SCREENSET).performScrollTo().performClick()
        log("ScreenSet button tapped — waiting for WebView...")

        fillScreenSetRegisterForm(email, password)
        log("Registration form submitted — waiting for TFA phone prompt...")

        fillTfaPhoneRegistration(testPhone)
        log("TFA phone submitted — fetching SMS code from extension store...")

        val smsCode = fetchSmsCode(testPhone)
        log("SMS code retrieved — entering into TFA code field")
        enterTfaCode(smsCode)

        // TFA completes in the WebView; on success the HostActivity dismisses and the SDK
        // navigates back to AccountScreen.
        waitForHostActivityDismissed(timeoutMillis = 30_000)
        waitForComposeNodeWithTag(TestTags.TEXT_UID, timeoutMillis = 30_000)
        composeRule.onNodeWithTag(TestTags.TEXT_UID).assertIsDisplayed()

        val uid = readUid()
        testUid = uid
        log("$UID_MARKER$uid")
        log("PASS test01_tfaRegister — AccountScreen shown with UID=$uid")
    }

    // region ScreenSet form helpers

    private fun fillScreenSetRegisterForm(email: String, password: String) {
        waitForWebViewAndIdle("post-screenset-render snapshot")

        val toRegister = device.wait(Until.findObject(By.textContains("Don't have an account")), 10_000)
        if (toRegister == null) {
            dumpHierarchy("register toggle link not found")
            fail("Could not find the 'Don't have an account yet?' link")
        }
        toRegister!!.click()
        log("Tapped switch-to-register link")
        device.waitForIdle()
        Thread.sleep(2_000)
        dumpHierarchy("register sub-screen snapshot")

        fillField(0, email, "Email")
        device.waitForIdle()
        Thread.sleep(3_000)

        fillField(1, "Sanity", "First name")
        fillField(2, "Tester", "Last name")

        // Scroll down to bring password/confirm fields into view — TFA site shows only
        // the first 3 fields initially. After scroll, the full 5-field form is in the tree.
        // Use ENTER to dismiss keyboard (not pressBack — pressBack closes HostActivity on TFA site).
        device.pressKeyCode(android.view.KeyEvent.KEYCODE_ENTER)
        device.waitForIdle()
        Thread.sleep(300)
        device.swipe(540, 1400, 540, 700, 10)
        device.waitForIdle()
        Thread.sleep(1_000)
        dumpHierarchy("after scroll — looking for password fields")

        // Use absolute indices 3 and 4 (password fields are the 4th and 5th EditTexts
        // in the register form — UIAutomator getText() for password fields returns ''
        // even when filled, so empty-field filtering is unreliable here).
        fillField(3, password, "Password")
        fillField(4, password, "Retype password")

        logFieldValues()
        tapSubmit("register form")
    }

    private fun fillScreenSetLoginForm(email: String, password: String) {
        waitForWebViewAndIdle("post-screenset-render snapshot (login)")
        dumpHierarchy("login sub-screen snapshot")

        fillField(0, email, "Email")
        fillField(1, password, "Password")

        logFieldValues()
        tapSubmit("login form")
        // Do NOT dismiss after login — on a TFA site the WebView transitions to the
        // TFA code entry screen next; dismissing here would interrupt that flow.
    }

    private fun fillField(index: Int, value: String, label: String) {
        repeat(3) { attempt ->
            val current = device.wait(Until.findObjects(By.clazz("android.widget.EditText")), 5_000)
            if (current == null || current.size <= index) {
                dumpHierarchy("field #$index ($label) not present (attempt $attempt)")
                if (attempt == 2) fail("Could not locate the $label field on the screenset form")
                Thread.sleep(1_000)
                return@repeat
            }
            try {
                val field = current!![index]
                field.click()
                device.waitForIdle()
                field.text = value
                device.waitForIdle()
                log("$label entered")
                return
            } catch (e: androidx.test.uiautomator.StaleObjectException) {
                log("$label field stale (attempt $attempt) — retrying...")
                Thread.sleep(500)
            }
        }
        fail("Could not fill the $label field (stale) on the screenset form")
    }

    private fun logFieldValues() {
        device.findObjects(By.clazz("android.widget.EditText")).forEachIndexed { i, f ->
            log("  field[$i] now = '${f.text}'")
        }
    }

    private fun tapSubmit(formLabel: String) {
        // Dismiss keyboard via ENTER (not pressBack — pressBack closes the HostActivity on TFA site).
        device.pressKeyCode(android.view.KeyEvent.KEYCODE_ENTER)
        device.waitForIdle()
        Thread.sleep(300)
        dumpHierarchy("post-keyboard-dismiss snapshot before submit ($formLabel)")

        // Scroll down to bring the Submit button into view.
        device.swipe(540, 1400, 540, 700, 10)
        device.waitForIdle()
        Thread.sleep(800)

        // Scroll more if terms checkbox is not yet visible.
        if (device.findObject(By.res("gigya-checkbox-terms")) == null) {
            device.swipe(540, 1400, 540, 700, 10)
            device.waitForIdle()
            Thread.sleep(500)
        }

        val termsCheckbox = device.findObject(By.res("gigya-checkbox-terms"))
        if (termsCheckbox != null && !termsCheckbox.isChecked) {
            log("Terms checkbox found and unchecked — checking it")
            termsCheckbox.click()
            device.waitForIdle()
            Thread.sleep(300)
        } else if (termsCheckbox == null) {
            log("Terms checkbox not found — continuing without checking it")
        }

        dumpHierarchy("pre-submit snapshot ($formLabel)")
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
        dumpHierarchy("post-submit snapshot ($formLabel)")
    }

    // endregion

    // region TFA native-screen helpers (Compose TestTags — TFAScreen, not WebView)

    /**
     * Fills the TFA phone-registration screen inside the ScreenSet WebView.
     *
     * Selects the first available country in the native <select> dialog (no scrolling needed),
     * then enters a draft local-format phone number. Because the extension intercepts the SMS
     * before any carrier is involved, the exact number and country don't matter — the extension
     * stores whatever key Gigya sends, and [fetchSmsCode] queries the same key.
     *
     * The [phoneNumber] value (from secrets.xml) is used as the lookup key for the extension
     * store and is also what gets entered into the phone field.
     */
    private fun fillTfaPhoneRegistration(phoneNumber: String) {
        log("Waiting for TFA phone registration screen in WebView...")
        val tfaScreenReady = device.wait(Until.hasObject(By.res("gigya-tfa-registration-screen")), 20_000)
        log("TFA registration screen present=$tfaScreenReady")
        device.waitForIdle()
        Thread.sleep(1_000)
        dumpHierarchy("TFA phone registration screen")

        // Select the first available country from the native <select> dialog.
        val countryDropdown = device.findObject(By.clazz("android.view.View").text("- Select -"))
        if (countryDropdown != null) {
            countryDropdown.click()
            device.waitForIdle()
            Thread.sleep(1_500)
            dumpHierarchy("country dialog open")

            // The dialog list starts with '- Select -' and a null entry before real countries.
            // Find all CheckedTextViews and click the first one with a non-blank, non-placeholder text.
            val allItems = device.wait(
                Until.findObjects(By.clazz("android.widget.CheckedTextView")), 5_000
            )
            val firstReal = allItems?.firstOrNull { item ->
                val t = item.text?.trim() ?: ""
                t.isNotEmpty() && t != "- Select -" && !t.startsWith("-")
            }
            if (firstReal != null) {
                val selectedCountry = firstReal.text.trim()
                firstReal.click()
                log("Country selected (first available): '$selectedCountry'")
                device.waitForIdle()
                Thread.sleep(500)
            } else {
                log("No real country entry found in dialog — pressing back")
                device.pressBack()
                device.waitForIdle()
            }
        } else {
            log("No country dropdown found — entering phone number directly")
        }

        // Enter the phone number in local format (no country code prefix — the dropdown
        // already sets the country code). E.164 +12015551234 → local 2015551234.
        val localPhone = if (phoneNumber.startsWith("+1")) phoneNumber.substring(2)
            else phoneNumber.trimStart('+').dropWhile { it.isDigit() && phoneNumber.indexOf(it) < 3 }
        val phoneField = device.wait(Until.findObject(By.clazz("android.widget.EditText")), 5_000)
        if (phoneField == null) {
            dumpHierarchy("TFA phone field not found")
            fail("Could not find the phone number field on TFA registration screen")
        }
        phoneField!!.click()
        device.waitForIdle()
        phoneField.text = localPhone
        device.waitForIdle()
        log("TFA phone number entered: $localPhone (E.164=$phoneNumber)")

        // Dismiss keyboard and tap "Get the Code".
        device.pressBack()
        device.waitForIdle()
        Thread.sleep(300)

        val getCodeBtn = device.findObject(By.clazz("android.widget.Button").text("Get the Code"))
            ?: device.findObject(By.clazz("android.widget.Button").textContains("Code"))
        if (getCodeBtn == null) {
            dumpHierarchy("TFA get-code button not found")
            fail("Could not find the 'Get the Code' button on TFA registration screen")
        }
        getCodeBtn!!.click()
        log("TFA 'Get the Code' button tapped — waiting 10s for extension to receive code...")
        device.waitForIdle()
        Thread.sleep(10_000)
    }

    /**
     * After the phone code has been sent, wait for the CodeEntry state and enter the OTP.
     */
    private fun enterTfaCode(code: String) {
        log("Waiting for TFA code entry screen in WebView...")
        // The code entry screen is still in the ScreenSet WebView.
        Thread.sleep(2_000) // brief wait for screen transition after "Get the Code"
        dumpHierarchy("TFA code entry screen")

        val codeField = device.wait(Until.findObject(By.clazz("android.widget.EditText")), 15_000)
        if (codeField == null) {
            dumpHierarchy("TFA code field not found")
            fail("Could not find the code entry field on TFA verification screen")
        }
        codeField!!.click()
        device.waitForIdle()
        codeField.text = code
        device.waitForIdle()
        log("TFA verification code entered: $code")

        device.pressBack()
        device.waitForIdle()
        Thread.sleep(300)

        val verifyBtn = device.findObject(By.clazz("android.widget.Button").text("Submit"))
            ?: device.findObject(By.clazz("android.widget.Button").text("submit"))
            ?: device.findObject(By.clazz("android.widget.Button").textContains("Verify"))
            ?: device.findObject(By.clazz("android.widget.Button").textContains("Submit"))
        if (verifyBtn == null) {
            dumpHierarchy("TFA verify/submit button not found")
            fail("Could not find the verify/submit button on TFA code entry screen")
        }
        verifyBtn!!.click()
        log("TFA verify button tapped")
        device.waitForIdle()
        Thread.sleep(5_000)
        dumpHierarchy("after TFA verify tap")

        // After TFA verify succeeds, the ScreenSet may navigate to a profile screen
        // or leave an empty WebView. Dismiss it so MainActivity can resume.
        dismissScreenSetAfterTfa()
    }

    // endregion

    // region SMS code interception

    /**
     * Polls the extension key-value store for the SMS OTP sent to [phoneNumber].
     * URL, token, and phone are all injected via secrets.xml — nothing is hardcoded here.
     */
    private fun fetchSmsCode(phoneNumber: String): String {
        val extensionUrl = SanityTestRunner.Secrets.extensionUrl
        val extensionToken = SanityTestRunner.Secrets.extensionToken
        if (extensionUrl.isBlank()) fail("tfa_extension_url is missing from secrets.xml — set TFA_EXTENSION_URL env var")
        if (extensionToken.isBlank()) fail("tfa_extension_token is missing from secrets.xml — set TFA_EXTENSION_TOKEN env var")

        val client = OkHttpClient()
        // Encode '+' manually to avoid double-encoding via URLEncoder.
        val encodedPhone = phoneNumber.replace("+", "%2B")
        val url = "$extensionUrl?key=$encodedPhone"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $extensionToken")
            .build()

        log("Polling extension store for SMS code (key=$encodedPhone)...")
        val deadline = System.currentTimeMillis() + 60_000
        while (System.currentTimeMillis() < deadline) {
            try {
                val body = client.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        log("  store poll HTTP ${resp.code} — retrying...")
                        return@use null
                    }
                    resp.body?.string()?.trim()
                }
                log("  store raw response: $body")
                if (!body.isNullOrBlank() && body != "[]") {
                    val arr = org.json.JSONArray(body as String)
                    if (arr.length() > 0) {
                        val entry = arr.getJSONObject(arr.length() - 1)
                        val code = entry.getJSONObject("value").getString("code").trim()
                        val logId = entry.optString("logId", "")
                        log("  SMS code extracted: '$code' (logId=$logId) — deleting from store")
                        // Delete the entry so stale codes can't be reused.
                        if (logId.isNotBlank()) {
                            runCatching {
                                val deleteRequest = Request.Builder()
                                    .url("$extensionUrl?callID=$logId&key=$encodedPhone")
                                    .delete()
                                    .addHeader("Authorization", "Bearer $extensionToken")
                                    .build()
                                client.newCall(deleteRequest).execute().use { r ->
                                    log("  store DELETE HTTP ${r.code}")
                                }
                            }.onFailure { log("  store DELETE error: ${it.message}") }
                        }
                        return code
                    }
                }
            } catch (e: Exception) {
                log("  store poll error: ${e.message} — retrying...")
            }
            log("  SMS code not yet available — retrying in 3s")
            Thread.sleep(3_000)
        }
        fail("Timed out waiting for SMS code in extension store")
        return ""
    }

    // endregion

    // region WebView helpers

    private fun waitForWebViewAndIdle(dumpReason: String) {
        val webViewReady = device.wait(Until.hasObject(By.clazz("android.webkit.WebView")), 40_000)
        log("WebView present=$webViewReady")
        device.waitForIdle()
        Thread.sleep(6_000)
        dumpHierarchy(dumpReason)
        if (webViewReady == null || !webViewReady) fail("ScreenSet WebView did not appear")
    }

    private fun dismissScreenSetAfterTfa() {
        // After TFA verification, the ScreenSet either auto-dismisses or leaves an empty/profile
        // WebView. Wait briefly for auto-dismiss; if still present, press Back.
        val deadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < deadline) {
            if (device.findObject(By.clazz("android.webkit.WebView")) == null) {
                log("ScreenSet dismissed after TFA verify")
                return
            }
            // Profile update screen — dismiss via back
            val saveBtn = device.findObject(By.clazz("android.widget.Button").text("Save"))
            if (saveBtn != null) {
                log("Profile screen after TFA — dismissing via back")
                device.pressBack(); device.waitForIdle()
                return
            }
            Thread.sleep(1_000)
        }
        // Still showing — force dismiss
        log("WebView still present after TFA verify — pressing back to dismiss")
        device.pressBack(); device.waitForIdle()
    }

    private fun waitForHostActivityDismissed(timeoutMillis: Long = 30_000) {
        val dismissed = device.wait(Until.gone(By.clazz("android.webkit.WebView")), timeoutMillis)
        log("HostActivity dismissed (WebView gone)=$dismissed")
        device.waitForIdle()
        Thread.sleep(500)
    }

    private fun waitForComposeNodeWithTag(tag: String, timeoutMillis: Long) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            try {
                composeRule.waitUntil(timeoutMillis = minOf(5_000L, deadline - System.currentTimeMillis())) {
                    composeRule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
                }
                return
            } catch (e: IllegalStateException) {
                log("Compose not ready yet: ${e.message}"); Thread.sleep(500)
            } catch (e: AssertionError) {
                if (System.currentTimeMillis() >= deadline) throw e
                Thread.sleep(500)
            }
        }
        fail("Timed out waiting for Compose node '$tag' after ${timeoutMillis}ms")
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
        private const val TAG = "TFASanitySuite"
        const val UID_MARKER = "SANITY_UID="

        var testUid: String = ""
    }

    private fun generateRandomUsPhone(): String {
        // Generate a random 10-digit US number in E.164 format.
        // Area code 200-999 (avoid 0xx/1xx), subscriber 7 digits.
        val areaCode = (200..999).random()
        val subscriber = (1000000..9999999).random()
        return "+1$areaCode$subscriber"
    }
}
