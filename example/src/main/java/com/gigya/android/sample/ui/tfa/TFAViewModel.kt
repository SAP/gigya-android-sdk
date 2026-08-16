package com.gigya.android.sample.ui.tfa

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gigya.android.sample.data.GigyaRepository
import com.gigya.android.sample.data.IGigyaRepository
import com.gigya.android.sample.data.TFAResolverState
import com.gigya.android.sdk.interruption.tfa.TFAResolverFactory
import com.gigya.android.sdk.interruption.tfa.models.TFAProviderModel
import com.gigya.android.sdk.tfa.GigyaDefinitions
import com.gigya.android.sdk.tfa.resolvers.IVerifyCodeResolver
import com.gigya.android.sdk.tfa.resolvers.totp.IVerifyTOTPResolver
import kotlinx.coroutines.launch

/**
 * ViewModel for [TFAScreen].
 *
 * Drives all TFA resolver steps: TOTP registration (QR code → verify),
 * phone registration (register phone → code sent → verify),
 * and phone verification (get registered phones → code sent → verify).
 *
 * The resolver objects from the login interruption are passed in via
 * [initialize] — they are held here for the multi-step flow duration.
 *
 * @param repository The [IGigyaRepository] implementation.
 */
class TFAViewModel(
    private val repository: IGigyaRepository = GigyaRepository(),
) : ViewModel() {

    var uiState by mutableStateOf<TFAUiState>(TFAUiState.Idle)
        private set

    private var resolverFactory: TFAResolverFactory? = null
    private var verifyCodeResolver: IVerifyCodeResolver? = null
    private var verifyTotpResolver: IVerifyTOTPResolver? = null

    /** Called by [TFAScreen] on entry to set the resolver and provider list. */
    fun initialize(providers: List<TFAProviderModel>, resolver: TFAResolverFactory) {
        resolverFactory = resolver
        uiState = TFAUiState.ProviderSelection(providers)
    }

    /** Called when the user selects a provider from the dropdown. */
    fun onProviderSelected(provider: TFAProviderModel, isRegistration: Boolean) {
        val factory = resolverFactory ?: return
        viewModelScope.launch {
            uiState = TFAUiState.Loading
            when (provider.name) {
                GigyaDefinitions.TFAProvider.TOTP -> {
                    if (isRegistration) {
                        when (val result = repository.tfaRegisterTotp(factory)) {
                            is TFAResolverState.QRCodeReady -> {
                                verifyTotpResolver = result.verifyResolver
                                uiState = TFAUiState.TOTPQRCode(result.qrCode)
                            }
                            is TFAResolverState.Error -> uiState = TFAUiState.Error(result.message)
                            else -> Unit
                        }
                    } else {
                        uiState = TFAUiState.CodeEntry
                    }
                }
                GigyaDefinitions.TFAProvider.PHONE,
                GigyaDefinitions.TFAProvider.LIVELINK -> {
                    if (isRegistration) {
                        uiState = TFAUiState.PhoneEntry
                    } else {
                        when (val result = repository.tfaGetRegisteredPhones(factory)) {
                            is TFAResolverState.PhoneCodeSent -> {
                                verifyCodeResolver = result.verifyResolver
                                uiState = TFAUiState.CodeEntry
                            }
                            is TFAResolverState.Error -> uiState = TFAUiState.Error(result.message)
                            else -> Unit
                        }
                    }
                }
                else -> uiState = TFAUiState.CodeEntry
            }
        }
    }

    /** Registers a phone number for TFA. */
    fun registerPhone(phoneNumber: String) {
        val factory = resolverFactory ?: return
        viewModelScope.launch {
            uiState = TFAUiState.Loading
            when (val result = repository.tfaRegisterPhone(factory, phoneNumber)) {
                is TFAResolverState.PhoneCodeSent -> {
                    verifyCodeResolver = result.verifyResolver
                    uiState = TFAUiState.CodeEntry
                }
                is TFAResolverState.Error -> uiState = TFAUiState.Error(result.message)
                else -> Unit
            }
        }
    }

    /** Verifies a code for phone TFA. */
    fun verifyPhoneCode(code: String) {
        val resolver = verifyCodeResolver ?: return
        viewModelScope.launch {
            uiState = TFAUiState.Loading
            when (val result = repository.tfaVerifyPhoneCode(resolver, code)) {
                is TFAResolverState.Resolved -> uiState = TFAUiState.Success
                is TFAResolverState.InvalidCode -> uiState = TFAUiState.Error("Invalid code — please try again")
                is TFAResolverState.Error -> uiState = TFAUiState.Error(result.message)
                else -> Unit
            }
        }
    }

    /** Verifies a TOTP code. */
    fun verifyTotpCode(code: String) {
        val resolver = verifyTotpResolver ?: return
        viewModelScope.launch {
            uiState = TFAUiState.Loading
            when (val result = repository.tfaVerifyTotpCode(resolver, code)) {
                is TFAResolverState.Resolved -> uiState = TFAUiState.Success
                is TFAResolverState.InvalidCode -> uiState = TFAUiState.Error("Invalid code — please try again")
                is TFAResolverState.Error -> uiState = TFAUiState.Error(result.message)
                else -> Unit
            }
        }
    }

    fun onNavigated() { uiState = TFAUiState.Idle }
}

/** All possible UI states for [TFAScreen]. */
sealed interface TFAUiState {
    data object Idle : TFAUiState
    data object Loading : TFAUiState
    data object Success : TFAUiState
    data object CodeEntry : TFAUiState
    data object PhoneEntry : TFAUiState
    data class ProviderSelection(val providers: List<TFAProviderModel>) : TFAUiState
    data class TOTPQRCode(val qrCode: String) : TFAUiState
    data class Error(val message: String) : TFAUiState
}
