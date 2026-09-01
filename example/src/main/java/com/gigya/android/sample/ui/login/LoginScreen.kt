package com.gigya.android.sample.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gigya.android.sample.model.MyAccount
import com.gigya.android.sample.ui.common.InputField
import com.gigya.android.sample.ui.common.PrimaryButton
import com.gigya.android.sample.ui.common.SectionTitle
import com.gigya.android.sample.ui.common.StatusRow
import com.gigya.android.sample.ui.common.TestTags
import com.gigya.android.sample.ui.theme.SampleTheme

/**
 * Login screen — the unauthenticated entry point.
 *
 * Stateless: all state lives in [LoginViewModel] and is passed in via
 * [uiState]. User interactions are forwarded as lambda callbacks so this
 * composable is fully previewable without a real ViewModel.
 *
 * Navigation side-effects are handled via [LaunchedEffect] reacting to
 * terminal [LoginUiState] values, keeping the composable pure.
 *
 * @param viewModel The [LoginViewModel] scoped to the nav graph.
 * @param onNavigateToAccount Called when login/register succeeds.
 * @param onNavigateToTFA Called when a TFA interruption is received.
 * @param onNavigateToLink Called when a link-accounts interruption is received.
 * @param onNavigateToOTP Called when the user taps "OTP Login".
 * @param onNavigateToSettings Called when the user taps the settings icon.
 */
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onNavigateToAccount: () -> Unit,
    onNavigateToTFA: () -> Unit,
    onNavigateToLink: () -> Unit,
    onNavigateToOTP: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activity = LocalContext.current as? FragmentActivity

    // Trigger biometric unlock automatically if session is locked on launch.
    LaunchedEffect(viewModel.uiState) {
        when (viewModel.uiState) {
            is LoginUiState.BiometricLocked -> {
                activity?.let { viewModel.biometricUnlock(it) }
            }
            is LoginUiState.BiometricUnlocked -> {
                viewModel.onNavigated()
                onNavigateToAccount()
            }
            is LoginUiState.Success -> {
                viewModel.onNavigated()
                onNavigateToAccount()
            }
            is LoginUiState.TFARequired -> onNavigateToTFA()
            is LoginUiState.LinkRequired -> onNavigateToLink()
            else -> Unit
        }
    }

    LoginScreenContent(
        uiState = viewModel.uiState,
        onLogin = viewModel::login,
        onRegister = viewModel::register,
        onSocialLogin = viewModel::socialLogin,
        onSsoLogin = viewModel::ssoLogin,
        onOtpLogin = onNavigateToOTP,
        onSettings = onNavigateToSettings,
        modifier = modifier,
    )
}

/**
 * Stateless content composable for [LoginScreen].
 *
 * Separated from the stateful wrapper so it can be driven directly in
 * `@Preview` functions without requiring a [LoginViewModel] instance.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreenContent(
    uiState: LoginUiState,
    onLogin: (email: String, password: String) -> Unit,
    onRegister: (email: String, password: String) -> Unit,
    onSocialLogin: (provider: String) -> Unit,
    onSsoLogin: () -> Unit,
    onOtpLogin: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var socialProvider by remember { mutableStateOf("") }

    val isLoading = uiState is LoginUiState.Loading
    val statusMessage = when (uiState) {
        is LoginUiState.Error -> uiState.message
        is LoginUiState.CaptchaRequired -> "Captcha required — please retry"
        else -> ""
    }
    val isError = uiState is LoginUiState.Error

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(),
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        if (isLoading) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Credentials ────────────────────────────────────────────────
            SectionTitle("Credentials")
            InputField(
                value = email,
                onValueChange = { email = it },
                label = "Email",
                tag = TestTags.INPUT_EMAIL,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            )
            InputField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                tag = TestTags.INPUT_PASSWORD,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation(),
            )
            PrimaryButton(
                text = "Login",
                onClick = { onLogin(email, password) },
                tag = TestTags.BTN_LOGIN,
                enabled = email.isNotBlank() && password.isNotBlank(),
            )
            PrimaryButton(
                text = "Register",
                onClick = { onRegister(email, password) },
                tag = TestTags.BTN_REGISTER,
                enabled = email.isNotBlank() && password.isNotBlank(),
            )

            // ── Passwordless ───────────────────────────────────────────────
            SectionTitle("Passwordless")
            PrimaryButton(
                text = "OTP Login",
                onClick = onOtpLogin,
                tag = TestTags.BTN_OTP_LOGIN,
            )

            // ── Social ─────────────────────────────────────────────────────
            SectionTitle("Social")
            InputField(
                value = socialProvider,
                onValueChange = { socialProvider = it },
                label = "Provider (e.g. facebook)",
                tag = TestTags.DROPDOWN_SOCIAL_PROVIDER,
            )
            PrimaryButton(
                text = "Social Login",
                onClick = { onSocialLogin(socialProvider) },
                tag = TestTags.BTN_SOCIAL_LOGIN,
                enabled = socialProvider.isNotBlank(),
            )

            // ── SSO ────────────────────────────────────────────────────────
            SectionTitle("SSO")
            PrimaryButton(
                text = "SSO Login",
                onClick = onSsoLogin,
                tag = TestTags.BTN_SSO,
            )

            // ── Status ─────────────────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            StatusRow(message = statusMessage, isError = isError)

            // Captcha: show a retry hint — the SDK handles the actual captcha
            // challenge internally; the user retries their login attempt.
            if (uiState is LoginUiState.CaptchaRequired) {
                PrimaryButton(
                    text = "Retry Login",
                    onClick = { onLogin(email, password) },
                    tag = TestTags.BTN_LOGIN,
                    enabled = email.isNotBlank() && password.isNotBlank(),
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// region Previews

@Preview(showBackground = true, name = "LoginScreen — Idle")
@Composable
private fun LoginScreenIdlePreview() {
    SampleTheme {
        LoginScreenContent(
            uiState = LoginUiState.Idle,
            onLogin = { _, _ -> },
            onRegister = { _, _ -> },
            onSocialLogin = {},
            onSsoLogin = {},
            onOtpLogin = {},
            onSettings = {},
        )
    }
}

@Preview(showBackground = true, name = "LoginScreen — Loading")
@Composable
private fun LoginScreenLoadingPreview() {
    SampleTheme {
        LoginScreenContent(
            uiState = LoginUiState.Loading,
            onLogin = { _, _ -> },
            onRegister = { _, _ -> },
            onSocialLogin = {},
            onSsoLogin = {},
            onOtpLogin = {},
            onSettings = {},
        )
    }
}

@Preview(showBackground = true, name = "LoginScreen — Error")
@Composable
private fun LoginScreenErrorPreview() {
    SampleTheme {
        LoginScreenContent(
            uiState = LoginUiState.Error("Error 403006: Invalid login credentials", 403006),
            onLogin = { _, _ -> },
            onRegister = { _, _ -> },
            onSocialLogin = {},
            onSsoLogin = {},
            onOtpLogin = {},
            onSettings = {},
        )
    }
}

// endregion
