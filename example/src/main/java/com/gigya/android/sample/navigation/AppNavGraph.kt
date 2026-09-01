package com.gigya.android.sample.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.gigya.android.sample.ui.account.AccountScreen
import com.gigya.android.sample.ui.account.AccountViewModel
import com.gigya.android.sample.ui.link.LinkAccountScreen
import com.gigya.android.sample.ui.link.LinkAccountViewModel
import com.gigya.android.sample.ui.login.LoginScreen
import com.gigya.android.sample.ui.login.LoginUiState
import com.gigya.android.sample.ui.login.LoginViewModel
import com.gigya.android.sample.ui.otp.OTPScreen
import com.gigya.android.sample.ui.otp.OTPViewModel
import com.gigya.android.sample.ui.settings.SettingsScreen
import com.gigya.android.sample.ui.settings.SettingsViewModel
import com.gigya.android.sample.ui.tfa.TFAScreen
import com.gigya.android.sample.ui.tfa.TFAViewModel

/**
 * Root navigation graph for the application.
 *
 * All destinations are declared here. Each screen receives only the
 * navigation callbacks it needs — screens never hold a reference to
 * [NavHostController] directly, keeping them fully stateless and previewable.
 *
 * The [loginViewModel] is shared with [TFAScreen] and [LinkAccountScreen]
 * so those screens can access interruption resolver state captured during
 * the login flow. Resolvers live in [LoginUiState] and must not be re-created.
 *
 * @param navController The [NavHostController] owned by [MainActivity].
 * @param loginViewModel The [LoginViewModel] scoped to the Activity.
 * @param startDestination The initial route. Defaults to [Screen.Login].
 * @param modifier Optional modifier applied to the [NavHost].
 */
@Composable
fun AppNavGraph(
    navController: NavHostController,
    loginViewModel: LoginViewModel,
    startDestination: String = Screen.Login.route,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = loginViewModel,
                onNavigateToAccount = {
                    navController.navigate(Screen.Account.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToTFA = { navController.navigate(Screen.TFA.route) },
                onNavigateToLink = { navController.navigate(Screen.LinkAccount.route) },
                onNavigateToOTP = { navController.navigate(Screen.OTP.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
            )
        }

        composable(Screen.Account.route) {
            val accountViewModel: AccountViewModel = viewModel()
            AccountScreen(
                viewModel = accountViewModel,
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Account.route) { inclusive = true }
                    }
                },
                onSettings = { navController.navigate(Screen.Settings.route) },
            )
        }

        composable(Screen.TFA.route) {
            val tfaViewModel: TFAViewModel = viewModel()
            val loginState = loginViewModel.uiState
            if (loginState is LoginUiState.TFARequired) {
                tfaViewModel.initialize(loginState.providers, loginState.resolver)
            }
            TFAScreen(
                viewModel = tfaViewModel,
                onSuccess = {
                    navController.navigate(Screen.Account.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.LinkAccount.route) {
            val linkViewModel: LinkAccountViewModel = viewModel()
            val loginState = loginViewModel.uiState
            if (loginState is LoginUiState.LinkRequired) {
                linkViewModel.initialize(loginState.accounts, loginState.resolver)
            }
            LinkAccountScreen(
                viewModel = linkViewModel,
                onSuccess = {
                    navController.navigate(Screen.Account.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.OTP.route) {
            val otpViewModel: OTPViewModel = viewModel()
            OTPScreen(
                viewModel = otpViewModel,
                onSuccess = {
                    navController.navigate(Screen.Account.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.Settings.route) {
            val settingsViewModel: SettingsViewModel = viewModel()
            SettingsScreen(
                viewModel = settingsViewModel,
                onReinitialized = { navController.popBackStack() },
            )
        }
    }
}
