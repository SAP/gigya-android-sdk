package com.gigya.android.sample.ui.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gigya.android.sample.data.GigyaRepository
import com.gigya.android.sample.data.GigyaSdkException
import com.gigya.android.sample.data.IGigyaRepository
import com.gigya.android.sample.data.LoginState
import com.gigya.android.sample.model.MyAccount
import com.gigya.android.sdk.interruption.link.ILinkAccountsResolver
import com.gigya.android.sdk.interruption.link.models.ConflictingAccounts
import com.gigya.android.sdk.interruption.tfa.TFAResolverFactory
import com.gigya.android.sdk.interruption.tfa.models.TFAProviderModel
import com.gigya.android.sdk.network.GigyaError
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * ViewModel for [LoginScreen].
 *
 * Owns the [LoginUiState] and drives all authentication flows by collecting
 * [Flow]-based and `suspend` repository methods. Never exposes SDK types
 * above this layer — all Gigya callbacks are bridged in [GigyaRepository].
 *
 * Interruption state ([LoginUiState.TFARequired], [LoginUiState.LinkRequired])
 * is also managed here because this ViewModel is scoped to the nav graph and
 * survives navigation to the TFA / Link screens, giving those screens access
 * to the in-progress resolver without requiring unsafe property injection.
 *
 * @param repository The [IGigyaRepository] implementation. Defaults to the
 *   production [GigyaRepository] so no DI framework is required.
 */
class LoginViewModel(
    private val repository: IGigyaRepository = GigyaRepository(),
) : ViewModel() {

    /** The single observable state for [LoginScreen]. */
    var uiState by mutableStateOf<LoginUiState>(LoginUiState.Idle)
        private set

    init {
        // If the session is biometrically locked on launch, surface the unlock prompt.
        val biometric = repository.biometricState
        if (biometric.isOptIn && biometric.isLocked) {
            uiState = LoginUiState.BiometricLocked
        }
    }

    // region Public actions

    /**
     * Unlocks a biometrically locked session on launch.
     * Called from [LoginScreen] via [LaunchedEffect] when [LoginUiState.BiometricLocked].
     */
    fun biometricUnlock(activity: FragmentActivity) {
        viewModelScope.launch {
            runCatching { repository.biometricUnlock(activity) }
                .onSuccess { uiState = LoginUiState.BiometricUnlocked }
                .onFailure { uiState = LoginUiState.Idle }
        }
    }

    /**
     * Initiates a credentials login flow.
     * Maps each [LoginState] emission to the corresponding [LoginUiState].
     */
    fun login(email: String, password: String) {
        uiState = LoginUiState.Loading
        viewModelScope.launch {
            repository.login(email, password)
                .catch { e -> uiState = e.toErrorState() }
                .collect { state -> uiState = state.toUiState() }
        }
    }

    /**
     * Initiates a credentials registration flow.
     * Same interruption semantics as [login].
     */
    fun register(email: String, password: String) {
        uiState = LoginUiState.Loading
        viewModelScope.launch {
            repository.register(email, password)
                .catch { e -> uiState = e.toErrorState() }
                .collect { state -> uiState = state.toUiState() }
        }
    }

    /**
     * Initiates a social login flow for the given [provider].
     * Example provider names: "facebook", "google", "line", "wechat".
     */
    fun socialLogin(provider: String) {
        uiState = LoginUiState.Loading
        viewModelScope.launch {
            repository.socialLogin(provider)
                .catch { e -> uiState = e.toErrorState() }
                .collect { state -> uiState = state.toUiState() }
        }
    }

    /**
     * Initiates a mobile SSO login flow.
     */
    fun ssoLogin() {
        uiState = LoginUiState.Loading
        viewModelScope.launch {
            repository.ssoLogin()
                .catch { e -> uiState = e.toErrorState() }
                .collect { state -> uiState = state.toUiState() }
        }
    }

    /**
     * Called by [LoginScreen] after it has consumed a [LoginUiState.Success]
     * and navigated to the account screen. Resets state to [LoginUiState.Idle]
     * so that back-navigation does not re-trigger the success handler.
     */
    fun onNavigated() {
        uiState = LoginUiState.Idle
    }

    // endregion

    // region Private helpers

    private fun LoginState.toUiState(): LoginUiState = when (this) {
        is LoginState.Success -> LoginUiState.Success(account)
        is LoginState.Error -> LoginUiState.Error(error.localizedMessage ?: "Unknown error", error.errorCode)
        is LoginState.Cancelled -> LoginUiState.Idle
        is LoginState.TFARegistrationRequired -> LoginUiState.TFARequired(providers, resolver)
        is LoginState.TFAVerificationRequired -> LoginUiState.TFARequired(providers, resolver)
        is LoginState.LinkRequired -> LoginUiState.LinkRequired(accounts, resolver)
        is LoginState.CaptchaRequired -> LoginUiState.CaptchaRequired
        is LoginState.OTPPending -> LoginUiState.Idle
    }

    private fun Throwable.toErrorState(): LoginUiState =
        if (this is GigyaSdkException) LoginUiState.Error(error.localizedMessage ?: "Unknown error", error.errorCode)
        else LoginUiState.Error(message ?: "Unknown error", -1)

    // endregion
}

/**
 * All possible UI states for [LoginScreen].
 *
 * Follows the sealed-interface + data-object/data-class pattern recommended
 * by Google's Now in Android architecture guide.
 */
sealed interface LoginUiState {

    /** Initial state — no operation in progress. */
    data object Idle : LoginUiState

    /** An SDK operation is in progress. */
    data object Loading : LoginUiState

    /** Authentication succeeded. The screen should navigate to [AccountScreen]. */
    data class Success(val account: MyAccount) : LoginUiState

    /** SDK returned an error. Display [message] in [StatusRow]. */
    data class Error(val message: String, val errorCode: Int) : LoginUiState

    /** TFA is required. Navigate to [TFAScreen]. */
    data class TFARequired(
        val providers: List<TFAProviderModel>,
        val resolver: TFAResolverFactory,
    ) : LoginUiState

    /** Account linking is required. Navigate to [LinkAccountScreen]. */
    data class LinkRequired(
        val accounts: ConflictingAccounts,
        val resolver: ILinkAccountsResolver,
    ) : LoginUiState

    /** Captcha is required before the login can proceed. */
    data object CaptchaRequired : LoginUiState

    /** Session is biometrically locked — prompt unlock before allowing login. */
    data object BiometricLocked : LoginUiState

    /** Biometric unlock succeeded — navigate to [AccountScreen]. */
    data object BiometricUnlocked : LoginUiState
}
