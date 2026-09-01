package com.gigya.android.sample.ui.account

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gigya.android.sample.data.BiometricState
import com.gigya.android.sample.data.GigyaRepository
import com.gigya.android.sample.data.GigyaSdkException
import com.gigya.android.sample.data.IGigyaRepository
import com.gigya.android.sample.data.SessionEvent
import com.gigya.android.sample.model.MyAccount
import com.gigya.android.sdk.biometric.GigyaBiometric
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * ViewModel for [AccountScreen].
 *
 * Manages all post-login SDK operations: account info, connections, passkeys,
 * biometric, push notifications, and session expiry observation.
 *
 * State is exposed as [AccountUiState] via [mutableStateOf] — idiomatic for
 * a pure Compose UI with no non-Compose observers.
 *
 * @param repository The [IGigyaRepository] implementation.
 */
class AccountViewModel(
    private val repository: IGigyaRepository = GigyaRepository(),
) : ViewModel() {

    /** The single observable state for [AccountScreen]. */
    var uiState by mutableStateOf<AccountUiState>(AccountUiState.Idle)
        private set

    /** Current biometric state — refreshed after every biometric operation. */
    var biometricState by mutableStateOf(repository.biometricState)
        private set

    init {
        observeSessionExpiry()
        getAccount()
    }

    // region Account

    /** Fetches the current account info and updates [AccountUiState.AccountLoaded]. */
    fun getAccount() {
        uiState = AccountUiState.Loading
        viewModelScope.launch {
            runCatching { repository.getAccount() }
                .onSuccess { uiState = AccountUiState.AccountLoaded(it) }
                .onFailure { uiState = it.toErrorState() }
        }
    }

    /** Logs out and signals [AccountUiState.LoggedOut] so the screen navigates away. */
    fun logout() {
        uiState = AccountUiState.Loading
        viewModelScope.launch {
            runCatching { repository.logout() }
                .onSuccess { uiState = AccountUiState.LoggedOut }
                .onFailure { uiState = it.toErrorState() }
        }
    }

    /** Called after the screen has consumed [AccountUiState.LoggedOut] and navigated. */
    fun onNavigated() {
        uiState = AccountUiState.Idle
    }

    // endregion

    // region Connections

    /** Adds the social [provider] connection to the current account. */
    fun addConnection(provider: String) {
        uiState = AccountUiState.Loading
        viewModelScope.launch {
            runCatching { repository.addConnection(provider) }
                .onSuccess { uiState = AccountUiState.Success("Connection added: $provider") }
                .onFailure { uiState = it.toErrorState() }
        }
    }

    /** Removes the social [provider] connection from the current account. */
    fun removeConnection(provider: String) {
        uiState = AccountUiState.Loading
        viewModelScope.launch {
            runCatching { repository.removeConnection(provider) }
                .onSuccess { uiState = AccountUiState.Success("Connection removed: $provider") }
                .onFailure { uiState = it.toErrorState() }
        }
    }

    // endregion

    // region Passkeys

    /** Registers a new WebAuthn passkey for the current account. */
    fun passkeyRegister(activity: FragmentActivity) {
        uiState = AccountUiState.Loading
        viewModelScope.launch {
            runCatching { repository.webAuthnRegister(activity.fidoResultHandler()) }
                .onSuccess { uiState = AccountUiState.Success("Passkey registered") }
                .onFailure { uiState = it.toErrorState() }
        }
    }

    /** Revokes all WebAuthn passkeys for the current account. */
    fun passkeyRevoke() {
        uiState = AccountUiState.Loading
        viewModelScope.launch {
            runCatching { repository.webAuthnRevoke() }
                .onSuccess { uiState = AccountUiState.Success("Passkey revoked") }
                .onFailure { uiState = it.toErrorState() }
        }
    }

    /** Fetches the list of registered WebAuthn credentials. */
    fun passkeyGetCredentials() {
        uiState = AccountUiState.Loading
        viewModelScope.launch {
            runCatching { repository.webAuthnGetCredentials() }
                .onSuccess { uiState = AccountUiState.Success(it) }
                .onFailure { uiState = it.toErrorState() }
        }
    }

    // endregion

    // region Biometric

    /** Refreshes the biometric state snapshot — call on every ON_RESUME. */
    fun refreshBiometricState() {
        biometricState = repository.biometricState
    }

    /** Opts the session into biometric protection. */
    fun biometricOptIn(activity: FragmentActivity) {
        viewModelScope.launch {
            runCatching { repository.biometricOptIn(activity) }
                .onSuccess {
                    biometricState = repository.biometricState
                    uiState = AccountUiState.Success("Biometric opt-in successful")
                }
                .onFailure { uiState = it.toErrorState() }
        }
    }

    /** Opts the session out of biometric protection. */
    fun biometricOptOut(activity: FragmentActivity) {
        viewModelScope.launch {
            runCatching { repository.biometricOptOut(activity) }
                .onSuccess {
                    biometricState = repository.biometricState
                    uiState = AccountUiState.Success("Biometric opt-out successful")
                }
                .onFailure { uiState = it.toErrorState() }
        }
    }

    /** Locks the current session. */
    fun biometricLock() {
        viewModelScope.launch {
            runCatching { repository.biometricLock() }
                .onSuccess {
                    biometricState = repository.biometricState
                    uiState = AccountUiState.Success("Session locked")
                }
                .onFailure { uiState = it.toErrorState() }
        }
    }

    /** Unlocks the current session using device biometric. */
    fun biometricUnlock(activity: FragmentActivity) {
        viewModelScope.launch {
            runCatching { repository.biometricUnlock(activity) }
                .onSuccess {
                    biometricState = repository.biometricState
                    uiState = AccountUiState.Success("Session unlocked")
                }
                .onFailure { uiState = it.toErrorState() }
        }
    }

    // endregion

    // region Push notifications

    /** Registers the device for push TFA notifications. */
    fun registerForPushTfa() {
        uiState = AccountUiState.Loading
        viewModelScope.launch {
            runCatching { repository.registerForPushTfa() }
                .onSuccess { uiState = AccountUiState.Success("Push TFA registered") }
                .onFailure { uiState = it.toErrorState() }
        }
    }

    /** Registers the device for push authentication notifications. */
    fun registerForPushAuth() {
        uiState = AccountUiState.Loading
        viewModelScope.launch {
            runCatching { repository.registerForPushAuth() }
                .onSuccess { uiState = AccountUiState.Success("Push auth registered") }
                .onFailure { uiState = it.toErrorState() }
        }
    }

    // endregion

    // region Session expiry

    /**
     * Observes [IGigyaRepository.sessionState] for [SessionEvent.Expired].
     * On expiry, transitions to [AccountUiState.SessionExpired] so the screen
     * can navigate back to login.
     */
    private fun observeSessionExpiry() {
        viewModelScope.launch {
            repository.sessionState
                .catch { /* observer errors are non-fatal */ }
                .collect { event ->
                    when (event) {
                        SessionEvent.Expired -> uiState = AccountUiState.SessionExpired
                    }
                }
        }
    }

    // endregion

    // region Private helpers

    private fun Throwable.toErrorState(): AccountUiState =
        if (this is GigyaSdkException) AccountUiState.Error(error.localizedMessage ?: "Unknown error", error.errorCode)
        else AccountUiState.Error(message ?: "Unknown error", -1)

    // endregion
}

/**
 * All possible UI states for [AccountScreen].
 */
sealed interface AccountUiState {

    /** No operation in progress. */
    data object Idle : AccountUiState

    /** An SDK operation is in progress. */
    data object Loading : AccountUiState

    /** Logout succeeded — screen should navigate to login. */
    data object LoggedOut : AccountUiState

    /** Session expired — screen should navigate to login. */
    data object SessionExpired : AccountUiState

    /** Account info fetched successfully. */
    data class AccountLoaded(val account: MyAccount) : AccountUiState

    /** An operation completed with a displayable result message. */
    data class Success(val message: String) : AccountUiState

    /** An SDK operation returned an error. */
    data class Error(val message: String, val errorCode: Int) : AccountUiState
}

/**
 * Extension to retrieve the FIDO result handler from an activity.
 *
 * The [ActivityResultLauncher] for FIDO intents is owned by [MainActivity]
 * and must be registered before [onStart]. This extension provides a safe
 * accessor — it returns `null` if the activity is not [MainActivity], which
 * the repository handles gracefully.
 *
 * SDK constraint: [ActivityResultLauncher] cannot be created in a ViewModel
 * or composable — it must live in the Activity. This is the recommended
 * pattern for bridging this platform requirement.
 */
private fun FragmentActivity.fidoResultHandler() =
    (this as? com.gigya.android.sample.ui.MainActivity)?.fidoResultHandler
        ?: throw IllegalStateException("fidoResultHandler only available from MainActivity")
