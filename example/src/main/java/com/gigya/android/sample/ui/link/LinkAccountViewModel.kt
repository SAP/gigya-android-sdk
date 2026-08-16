package com.gigya.android.sample.ui.link

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.gigya.android.sample.data.GigyaRepository
import com.gigya.android.sample.data.IGigyaRepository
import com.gigya.android.sdk.interruption.link.ILinkAccountsResolver
import com.gigya.android.sdk.interruption.link.models.ConflictingAccounts

/**
 * ViewModel for [LinkAccountScreen].
 *
 * Holds the [ILinkAccountsResolver] from the login interruption and drives
 * the link-to-site or link-to-social operations. Linking resumes the original
 * login flow via the resolver's internal callback — no explicit success state
 * is needed here; the [LoginViewModel] uiState will update to [Success].
 *
 * @param repository The [IGigyaRepository] implementation.
 */
class LinkAccountViewModel(
    private val repository: IGigyaRepository = GigyaRepository(),
) : ViewModel() {

    var uiState by mutableStateOf<LinkUiState>(LinkUiState.Idle)
        private set

    private var resolver: ILinkAccountsResolver? = null
    private var accounts: ConflictingAccounts? = null

    /** Called on entry to set the resolver and conflicting accounts info. */
    fun initialize(conflictingAccounts: ConflictingAccounts, linkResolver: ILinkAccountsResolver) {
        accounts = conflictingAccounts
        resolver = linkResolver
        uiState = LinkUiState.Ready(
            providers = conflictingAccounts.loginProviders ?: emptyList(),
        )
    }

    /** Links to an existing site account using credentials. */
    fun linkToSite(loginId: String, password: String) {
        val res = resolver ?: return
        uiState = LinkUiState.Loading
        repository.linkToSite(res, loginId, password)
        // The login callback fires the result — LoginViewModel.uiState updates to Success
    }

    /** Links to an existing social account. */
    fun linkToSocial(provider: String) {
        val res = resolver ?: return
        uiState = LinkUiState.Loading
        repository.linkToSocial(res, provider)
    }
}

/** All possible UI states for [LinkAccountScreen]. */
sealed interface LinkUiState {
    data object Idle : LinkUiState
    data object Loading : LinkUiState
    data class Ready(val providers: List<String>) : LinkUiState
    data class Error(val message: String) : LinkUiState
}
