package com.gigya.android.sample.ui.account

import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.gigya.android.sample.data.BiometricState
import com.gigya.android.sample.model.MyAccount
import com.gigya.android.sample.ui.common.InputField
import com.gigya.android.sample.ui.common.PrimaryButton
import com.gigya.android.sample.ui.common.SectionTitle
import com.gigya.android.sample.ui.common.StatusRow
import com.gigya.android.sample.ui.common.TestTags
import com.gigya.android.sample.ui.theme.SampleTheme

/**
 * Account screen — the authenticated home screen.
 *
 * Displayed after any successful login. Provides access to all post-login
 * SDK operations: account info, social connections, passkeys, biometric
 * session management, and push notification registration.
 *
 * Stateless: all state is owned by [AccountViewModel] and passed via
 * [viewModel]. Navigation side-effects are handled via [LaunchedEffect].
 *
 * @param viewModel The [AccountViewModel] scoped to this destination.
 * @param onLogout Called when logout succeeds or the session expires.
 * @param onSettings Called when the user taps the settings icon.
 */
@Composable
fun AccountScreen(
    viewModel: AccountViewModel,
    onLogout: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activity = LocalContext.current as? FragmentActivity
    val lifecycleOwner = LocalLifecycleOwner.current

    // Navigate away on logout or session expiry.
    LaunchedEffect(viewModel.uiState) {
        when (viewModel.uiState) {
            is AccountUiState.LoggedOut,
            is AccountUiState.SessionExpired -> {
                viewModel.onNavigated()
                onLogout()
            }
            else -> Unit
        }
    }

    // Regression fix: re-check biometric lock state on every ON_RESUME.
    // When the app returns from background with a locked session, prompt
    // unlock immediately rather than showing stale account data.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshBiometricState()
                val state = viewModel.biometricState
                if (state.isOptIn && state.isLocked) {
                    activity?.let { viewModel.biometricUnlock(it) }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AccountScreenContent(
        uiState = viewModel.uiState,
        biometricState = viewModel.biometricState,
        onGetAccount = viewModel::getAccount,
        onLogout = viewModel::logout,
        onAddConnection = viewModel::addConnection,
        onRemoveConnection = viewModel::removeConnection,
        onPasskeyRegister = { activity?.let { viewModel.passkeyRegister(it) } },
        onPasskeyRevoke = viewModel::passkeyRevoke,
        onPasskeyGetCredentials = viewModel::passkeyGetCredentials,
        onBiometricOpt = { activity?.let { act ->
            if (viewModel.biometricState.isOptIn) viewModel.biometricOptOut(act)
            else viewModel.biometricOptIn(act)
        }},
        onBiometricLockToggle = { activity?.let { act ->
            if (viewModel.biometricState.isLocked) viewModel.biometricUnlock(act)
            else viewModel.biometricLock()
        }},
        onPushTfaOptIn = viewModel::registerForPushTfa,
        onPushAuthOptIn = viewModel::registerForPushAuth,
        onSettings = onSettings,
        modifier = modifier,
    )
}

/**
 * Stateless content composable for [AccountScreen].
 *
 * Separated from the stateful wrapper so it can be driven directly in
 * `@Preview` functions without requiring a [AccountViewModel] instance.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreenContent(
    uiState: AccountUiState,
    biometricState: BiometricState,
    onGetAccount: () -> Unit,
    onLogout: () -> Unit,
    onAddConnection: (String) -> Unit,
    onRemoveConnection: (String) -> Unit,
    onPasskeyRegister: () -> Unit,
    onPasskeyRevoke: () -> Unit,
    onPasskeyGetCredentials: () -> Unit,
    onBiometricOpt: () -> Unit,
    onBiometricLockToggle: () -> Unit,
    onPushTfaOptIn: () -> Unit,
    onPushAuthOptIn: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var connectionProvider by remember { mutableStateOf("") }

    val isLoading = uiState is AccountUiState.Loading
    val uid = (uiState as? AccountUiState.AccountLoaded)?.account?.uid ?: ""
    val statusMessage = when (uiState) {
        is AccountUiState.Success -> uiState.message
        is AccountUiState.Error -> uiState.message
        is AccountUiState.SessionExpired -> "Session expired"
        else -> ""
    }
    val isError = uiState is AccountUiState.Error || uiState is AccountUiState.SessionExpired

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account", style = MaterialTheme.typography.titleMedium) },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
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

            // ── Account Info ───────────────────────────────────────────────
            SectionTitle("Account")
            if (uid.isNotEmpty()) {
                Text(
                    text = "UID: $uid",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.TEXT_UID),
                )
            }
            PrimaryButton(
                text = "Get Account Info",
                onClick = onGetAccount,
                tag = TestTags.BTN_GET_ACCOUNT,
            )
            PrimaryButton(
                text = "Logout",
                onClick = onLogout,
                tag = TestTags.BTN_LOGOUT,
            )

            // ── Social Connections ─────────────────────────────────────────
            SectionTitle("Social Connections")
            InputField(
                value = connectionProvider,
                onValueChange = { connectionProvider = it },
                label = "Provider (e.g. facebook)",
                tag = TestTags.DROPDOWN_CONNECTION_PROVIDER,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton(
                    text = "Add",
                    onClick = { onAddConnection(connectionProvider) },
                    tag = TestTags.BTN_ADD_CONNECTION,
                    enabled = connectionProvider.isNotBlank(),
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(
                    text = "Remove",
                    onClick = { onRemoveConnection(connectionProvider) },
                    tag = TestTags.BTN_REMOVE_CONNECTION,
                    enabled = connectionProvider.isNotBlank(),
                    modifier = Modifier.weight(1f),
                )
            }

            // ── Passkeys ───────────────────────────────────────────────────
            SectionTitle("Passkeys")
            PrimaryButton(
                text = "Register Passkey",
                onClick = onPasskeyRegister,
                tag = TestTags.BTN_PASSKEY_REGISTER,
            )
            PrimaryButton(
                text = "Revoke Passkey",
                onClick = onPasskeyRevoke,
                tag = TestTags.BTN_PASSKEY_REVOKE,
            )
            PrimaryButton(
                text = "Get Credentials",
                onClick = onPasskeyGetCredentials,
                tag = TestTags.BTN_PASSKEY_GET,
            )

            // ── Biometric ──────────────────────────────────────────────────
            SectionTitle("Biometric")
            BiometricStatusRow(biometricState = biometricState)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton(
                    text = if (biometricState.isOptIn) "Opt Out" else "Opt In",
                    onClick = onBiometricOpt,
                    tag = TestTags.BTN_BIOMETRIC_OPT,
                    enabled = biometricState.isAvailable,
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(
                    text = if (biometricState.isLocked) "Unlock" else "Lock",
                    onClick = onBiometricLockToggle,
                    tag = TestTags.BTN_BIOMETRIC_LOCK,
                    enabled = biometricState.isOptIn,
                    modifier = Modifier.weight(1f),
                )
            }

            // ── Push Notifications ─────────────────────────────────────────
            SectionTitle("Push Notifications")
            PrimaryButton(
                text = "Push TFA Opt-In",
                onClick = onPushTfaOptIn,
                tag = TestTags.BTN_PUSH_TFA_OPT_IN,
            )
            PrimaryButton(
                text = "Push Auth Opt-In",
                onClick = onPushAuthOptIn,
                tag = TestTags.BTN_PUSH_AUTH_OPT_IN,
            )

            // ── Status ─────────────────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            StatusRow(message = statusMessage, isError = isError)
            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * Displays the current biometric opt-in / lock status as a single line of text.
 *
 * @param biometricState The current biometric state snapshot.
 */
@Composable
fun BiometricStatusRow(
    biometricState: BiometricState,
    modifier: Modifier = Modifier,
) {
    val status = when {
        !biometricState.isAvailable -> "Biometric: not available on this device"
        !biometricState.isOptIn -> "Biometric: not enrolled"
        biometricState.isLocked -> "Biometric: session locked"
        else -> "Biometric: session active"
    }
    Text(
        text = status,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier
            .fillMaxWidth()
            .testTag(TestTags.TEXT_BIOMETRIC_STATUS),
    )
}

// region Previews

@Preview(showBackground = true, name = "AccountScreen — Idle")
@Composable
private fun AccountScreenIdlePreview() {
    SampleTheme {
        AccountScreenContent(
            uiState = AccountUiState.Idle,
            biometricState = BiometricState(isAvailable = true, isOptIn = false, isLocked = false),
            onGetAccount = {}, onLogout = {}, onAddConnection = {}, onRemoveConnection = {},
            onPasskeyRegister = {}, onPasskeyRevoke = {}, onPasskeyGetCredentials = {},
            onBiometricOpt = {}, onBiometricLockToggle = {},
            onPushTfaOptIn = {}, onPushAuthOptIn = {}, onSettings = {},
        )
    }
}

@Preview(showBackground = true, name = "AccountScreen — Account Loaded")
@Composable
private fun AccountScreenLoadedPreview() {
    SampleTheme {
        AccountScreenContent(
            uiState = AccountUiState.AccountLoaded(MyAccount().apply { uid = "abc123def456" }),
            biometricState = BiometricState(isAvailable = true, isOptIn = true, isLocked = false),
            onGetAccount = {}, onLogout = {}, onAddConnection = {}, onRemoveConnection = {},
            onPasskeyRegister = {}, onPasskeyRevoke = {}, onPasskeyGetCredentials = {},
            onBiometricOpt = {}, onBiometricLockToggle = {},
            onPushTfaOptIn = {}, onPushAuthOptIn = {}, onSettings = {},
        )
    }
}

@Preview(showBackground = true, name = "AccountScreen — Error")
@Composable
private fun AccountScreenErrorPreview() {
    SampleTheme {
        AccountScreenContent(
            uiState = AccountUiState.Error("Error 403047: Permission denied", 403047),
            biometricState = BiometricState(isAvailable = false, isOptIn = false, isLocked = false),
            onGetAccount = {}, onLogout = {}, onAddConnection = {}, onRemoveConnection = {},
            onPasskeyRegister = {}, onPasskeyRevoke = {}, onPasskeyGetCredentials = {},
            onBiometricOpt = {}, onBiometricLockToggle = {},
            onPushTfaOptIn = {}, onPushAuthOptIn = {}, onSettings = {},
        )
    }
}

@Preview(showBackground = true, name = "BiometricStatusRow")
@Composable
private fun BiometricStatusRowPreview() {
    SampleTheme {
        BiometricStatusRow(
            biometricState = BiometricState(isAvailable = true, isOptIn = true, isLocked = true),
        )
    }
}

// endregion
