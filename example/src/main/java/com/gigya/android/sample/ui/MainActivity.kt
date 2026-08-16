package com.gigya.android.sample.ui

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
import com.gigya.android.sdk.auth.passkeys.PasskeysAuthenticationProvider
import java.lang.ref.WeakReference

/**
 * Single host activity for the application.
 *
 * Responsibilities:
 * - Owns the [ActivityResultLauncher] for FIDO/WebAuthn intent results.
 *   This must be registered before [onStart], so it lives here rather than
 *   in a ViewModel or composable.
 * - Hosts the [AppNavGraph] via [setContent].
 * - Determines the start destination based on session state.
 *
 * All navigation logic lives in [AppNavGraph]. All SDK interaction lives in
 * [GigyaRepository]. This activity is intentionally thin.
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

    private val loginViewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Configure passkey authentication provider — requires a WeakReference
        // to avoid leaking the Activity through the SDK's internal reference.
        Gigya.getInstance().setPasskeyAuthenticatorProvider(
            PasskeysAuthenticationProvider(WeakReference(this))
        )

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
}
