package com.gigya.android.sample.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.gigya.android.sample.ui.account.AccountScreen
import com.gigya.android.sample.ui.link.LinkAccountScreen
import com.gigya.android.sample.ui.login.LoginScreen
import com.gigya.android.sample.ui.login.LoginViewModel
import com.gigya.android.sample.ui.otp.OTPScreen
import com.gigya.android.sample.ui.settings.SettingsScreen
import com.gigya.android.sample.ui.tfa.TFAScreen

/**
 * Root navigation graph for the application.
 *
 * All destinations are declared here. Each screen receives only the
 * navigation callbacks it needs — screens never hold a reference to
 * [NavHostController] directly, keeping them fully stateless and previewable.
 *
 * @param navController The [NavHostController] owned by [MainActivity].
 * @param loginViewModel The [LoginViewModel] scoped to the nav graph, shared
 *   between [LoginScreen], [TFAScreen], and [LinkAccountScreen] so that
 *   interruption resolver state survives navigation between these destinations.
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
            AccountScreen(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Account.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.TFA.route) {
            TFAScreen(
                onSuccess = {
                    navController.navigate(Screen.Account.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.LinkAccount.route) {
            LinkAccountScreen(
                onSuccess = {
                    navController.navigate(Screen.Account.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.OTP.route) {
            OTPScreen(
                onSuccess = {
                    navController.navigate(Screen.Account.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onReinitialized = { navController.popBackStack() },
            )
        }
    }
}
