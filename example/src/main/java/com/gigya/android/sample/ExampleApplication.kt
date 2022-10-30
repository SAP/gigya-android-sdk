package com.gigya.android.sample

import android.app.Application
import android.content.pm.PackageManager
import android.util.Base64
import android.util.Log
import android.webkit.WebView
import com.gigya.android.sample.model.MyAccount
import com.gigya.android.sdk.Gigya
import com.gigya.android.sdk.GigyaLogger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class ExampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Allow WebViews debugging.
        WebView.setWebContentsDebuggingEnabled(true)

        // Gigya logs.
        GigyaLogger.setDebugMode(true)

        // This step is crucial for the SDK lifecycle. This will bing the application
        // context.
        Gigya.setApplication(this)

        // Secure all SDK activities (prevent screenshots).
        Gigya.secureActivityWindow(true)

        // Initialize the Gigya SDK using custom account scheme.
        Gigya.getInstance(MyAccount::class.java)
    }

    // Call function to get the application signature used for Facebook login or Fido authentication.
    private fun getSignature() {
        try {
            val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            for (signature in info.signatures) {
                val md: MessageDigest = MessageDigest.getInstance("SHA256")
                md.update(signature.toByteArray())
                Log.e("MY KEY HASH:", Base64.encodeToString(md.digest(),
                        Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP))
            }
        } catch (e: PackageManager.NameNotFoundException) {
        } catch (e: NoSuchAlgorithmException) {
        }
    }
}
