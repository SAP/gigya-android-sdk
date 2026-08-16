package com.gigya.android.sample.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.gigya.android.sample.data.GigyaRepository
import com.gigya.android.sample.navigation.AppNavGraph
import com.gigya.android.sample.navigation.Screen
import com.gigya.android.sample.ui.login.LoginViewModel
import com.gigya.android.sample.ui.theme.SampleTheme
import com.gigya.android.sdk.Gigya
import com.gigya.android.sdk.auth.GigyaAuth
import com.gigya.android.sdk.auth.passkeys.PasskeysAuthenticationProvider
import java.lang.ref.WeakReference

/**
 * Single host activity for the application.
 *
 * Responsibilities:
 * - Owns the [ActivityResultLauncher] for FIDO/WebAuthn intent results.
 *   Must be registered before [onStart] — platform constraint.
 * - Requests POST_NOTIFICATIONS permission on Android 13+ for push TFA/auth.
 * - Calls [GigyaAuth.registerForPushNotifications] on start to ensure the
 *   FCM token is registered with the Gigya backend.
 * - Hosts the [AppNavGraph] via [setContent].
 * - Determines the start destination based on session state.
 */
class MainActivity : ComponentActivity() {

    /**
     * FIDO/WebAuthn result handler.
     *
     * [ActivityResultLauncher] must be registered before [onStart], which
     * means it cannot live in a ViewModel or be created lazily inside a
     * composable. It is registered here and passed to ViewModels that need it.
     */
    val fidoResultHandler: ActivityResultLauncher<IntentSenderRequest> =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            Gigya.getInstance().WebAuthn().handleFidoResult(result)
        }

    /**
     * POST_NOTIFICATIONS permission launcher (Android 13+).
     * Result is ignored — push opt-in/out is handled by the user via
     * AccountScreen buttons; this only satisfies the system requirement.
     */
    private val notificationsPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    private val loginViewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        Gigya.getInstance().setPasskeyAuthenticatorProvider(
            PasskeysAuthenticationProvider(WeakReference(this))
        )

        requestNotificationsPermissionIfNeeded()

        val startDestination = if (GigyaRepository().isLoggedIn) {
            Screen.Account.route
        } else {
            Screen.Login.route
        }

        setContent {
            SampleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val navController = rememberNavController()
                    AppNavGraph(
                        navController = navController,
                        loginViewModel = loginViewModel,
                        startDestination = startDestination,
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Register for push notifications on every start so the FCM token
        // is always current. GigyaAuth handles deduplication internally.
        GigyaAuth.getInstance().registerForPushNotifications(this)
    }

    /**
     * Requests POST_NOTIFICATIONS permission on Android 13+ (API 33+).
     * On older versions the permission is granted automatically.
     */
    private fun requestNotificationsPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
