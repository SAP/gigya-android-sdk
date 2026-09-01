package com.gigya.android.sample.ui.otp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gigya.android.sample.data.GigyaRepository
import com.gigya.android.sample.data.IGigyaRepository
import com.gigya.android.sample.data.LoginState
import com.gigya.android.sample.model.MyAccount
import com.gigya.android.sdk.auth.resolvers.IGigyaOtpResult
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * ViewModel for [OTPScreen].
 *
 * Drives the two-phase phone OTP login flow:
 * 1. [sendCode] — calls [IGigyaRepository.otpLogin], emits [OTPUiState.PendingVerification]
 *    when the SMS is sent, holding the [IGigyaOtpResult] resolver.
 * 2. [verifyCode] — calls [IGigyaRepository.otpVerify], the original [GigyaOTPCallback]
 *    then fires success/error back into the [otpLogin] flow.
 *
 * @param repository The [IGigyaRepository] implementation.
 */
class OTPViewModel(
    private val repository: IGigyaRepository = GigyaRepository(),
) : ViewModel() {

    var uiState by mutableStateOf<OTPUiState>(OTPUiState.Idle)
        private set

    private var otpResolver: IGigyaOtpResult? = null

    /** Initiates the OTP login — sends an SMS to [phoneNumber]. */
    fun sendCode(phoneNumber: String) {
        uiState = OTPUiState.Loading
        viewModelScope.launch {
            repository.otpLogin(phoneNumber)
                .catch { e -> uiState = OTPUiState.Error(e.message ?: "Unknown error") }
                .collect { state ->
                    when (state) {
                        is LoginState.OTPPending -> {
                            otpResolver = state.resolver
                            uiState = OTPUiState.PendingVerification
                        }
                        is LoginState.Success -> uiState = OTPUiState.Success(state.account)
                        is LoginState.Error -> uiState = OTPUiState.Error(state.error.localizedMessage ?: "OTP error")
                        else -> Unit
                    }
                }
        }
    }

    /** Submits the [code] to complete OTP verification. */
    fun verifyCode(code: String) {
        val resolver = otpResolver ?: return
        uiState = OTPUiState.Loading
        repository.otpVerify(resolver, code)
        // Result fires back through the otpLogin flow collector above
    }

    fun onNavigated() { uiState = OTPUiState.Idle }
}

/** All possible UI states for [OTPScreen]. */
sealed interface OTPUiState {
    data object Idle : OTPUiState
    data object Loading : OTPUiState
    data object PendingVerification : OTPUiState
    data class Success(val account: MyAccount) : OTPUiState
    data class Error(val message: String) : OTPUiState
}
