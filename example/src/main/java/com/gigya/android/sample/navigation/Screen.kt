package com.gigya.android.sample.navigation

/**
 * All navigation destinations in the app.
 *
 * Each object's [route] string is used as the [androidx.navigation.NavGraph]
 * destination identifier. Defining routes here — rather than as inline string
 * literals — prevents typos and makes refactoring safe.
 */
sealed class Screen(val route: String) {

    /** Unauthenticated entry point. Shown when no session is active. */
    data object Login : Screen("login")

    /** Post-login home screen. Requires an active session. */
    data object Account : Screen("account")

    /** Two-factor authentication flow (TOTP, phone, email). */
    data object TFA : Screen("tfa")

    /** Account-linking flow triggered by a conflicting-accounts interruption. */
    data object LinkAccount : Screen("link_account")

    /** Phone OTP login flow. */
    data object OTP : Screen("otp")

    /** SDK re-initialisation screen. */
    data object Settings : Screen("settings")
}
