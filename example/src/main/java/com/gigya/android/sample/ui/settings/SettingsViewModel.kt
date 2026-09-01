package com.gigya.android.sample.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.gigya.android.sample.data.GigyaRepository
import com.gigya.android.sample.data.IGigyaRepository

/**
 * ViewModel for [SettingsScreen].
 *
 * Drives SDK re-initialisation with a new API key / data center.
 * This is a fire-and-forget synchronous operation — the SDK invalidates
 * the current session and re-initialises immediately.
 *
 * @param repository The [IGigyaRepository] implementation.
 */
class SettingsViewModel(
    private val repository: IGigyaRepository = GigyaRepository(),
) : ViewModel() {

    var uiState by mutableStateOf<SettingsUiState>(SettingsUiState.Idle)
        private set

    /** Re-initialises the SDK with the provided credentials. */
    fun reinitialize(apiKey: String, dataCenter: String, cname: String) {
        runCatching {
            repository.reinitializeSdk(
                apiKey = apiKey,
                dataCenter = dataCenter.ifBlank { null },
                cname = cname.ifBlank { null },
            )
        }.onSuccess {
            uiState = SettingsUiState.Success
        }.onFailure {
            uiState = SettingsUiState.Error(it.message ?: "Re-initialisation failed")
        }
    }

    fun onNavigated() { uiState = SettingsUiState.Idle }
}

/** All possible UI states for [SettingsScreen]. */
sealed interface SettingsUiState {
    data object Idle : SettingsUiState
    data object Success : SettingsUiState
    data class Error(val message: String) : SettingsUiState
}
