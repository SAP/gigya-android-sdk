package com.gigya.android.sample.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.gigya.android.sample.R
import com.gigya.android.sample.extras.displayErrorAlert
import com.gigya.android.sample.extras.gone
import com.gigya.android.sample.extras.loadRoundImageWith
import com.gigya.android.sample.extras.visible
import com.gigya.android.sample.model.MyAccount
import com.gigya.android.sample.ui.fragment.BackPressListener
import com.gigya.android.sample.ui.fragment.ConflictingAccountsDialog
import com.gigya.android.sample.ui.fragment.InputDialog
import com.gigya.android.sdk.Gigya
import com.gigya.android.sdk.GigyaDefinitions
import com.gigya.android.sdk.biometric.GigyaBiometric
import com.gigya.android.sdk.biometric.GigyaPromptInfo
import com.gigya.android.sdk.biometric.IGigyaBiometricCallback
import com.gigya.android.sdk.interruption.link.models.ConflictingAccounts
import com.gigya.android.sdk.interruption.tfa.TFAResolverFactory
import com.gigya.android.sdk.interruption.tfa.models.TFAProviderModel
import com.gigya.android.sdk.network.GigyaError
import com.gigya.android.sdk.tfa.ui.TFAProviderSelectionFragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.nav_header_main.*
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.toast
import java.util.*


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, InputDialog.IApiResultCallback {

    private var viewModel: MainViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        title = "Gigya SDK sample"
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        setSupportActionBar(toolbar)
        initDrawer()
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter()
        filter.addAction(GigyaDefinitions.Broadcasts.INTENT_ACTION_SESSION_EXPIRED)
        filter.addAction(GigyaDefinitions.Broadcasts.INTENT_ACTION_SESSION_INVALID)
        LocalBroadcastManager.getInstance(this).registerReceiver(sessionLifecycleReceiver,
                filter)
        // Evaluate fingerprint session.
        evaluateFingerprintSession()
        // Register for myAccountLiveData info updates.
        registerAccountUpdates()

        /* If we are already logged in - get myAccountLiveData info and update relevant myAccountLiveData UI (drawer header). */
        if (viewModel!!.isLoggedIn()) {
            onGetAccount()
        }
    }

    override fun onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(sessionLifecycleReceiver)
        unregisterAccountUpdates()
        super.onPause()
    }

    private
    val sessionLifecycleReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            action?.let { intent_action ->
                viewModel?.flushAccountReferences()
                if (!isDestroyed && !isFinishing) {
                    invalidateAccountData()
                    invalidateOptionsMenu()
                    val message: String = when (intent_action) {
                        GigyaDefinitions.Broadcasts.INTENT_ACTION_SESSION_EXPIRED -> "Your session has expired"
                        GigyaDefinitions.Broadcasts.INTENT_ACTION_SESSION_INVALID -> "Your session is invalid"
                        else -> ""
                    }
                    displayErrorAlert("Alert", message)
                }
            }
        }
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
            return
        }
        // Trigger on back press trigger in the current fragment.
        supportFragmentManager.findFragmentById(R.id.frag_container)?.let { fragment ->
            if (fragment is BackPressListener) {
                (fragment as BackPressListener).onBackPressed()
            }
        }
        super.onBackPressed()
    }

    //region DRAWER SETUP

    private fun initDrawer() {
        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        /* Setup drawer navigation header click listener. */
        nav_view.getHeaderView(0)?.setOnClickListener {
            if (viewModel!!.isLoggedIn()) {
                showAccountDetails()
            }
        }
        viewModel?.uiTrigger?.observe(this, Observer { dataPair ->
            @Suppress("UNCHECKED_CAST")
            when (dataPair?.first) {
                MainViewModel.UI_TRIGGER_SHOW_CONFLICTING_ACCOUNTS -> onConflictingAccounts(dataPair.second as ConflictingAccounts)
                MainViewModel.UI_TRIGGER_SHOW_TFA_PROVIDER_SELECTION_FOR_REGISTRATION ->
                    onTFARegistrationProviderSelection(dataPair.second as Pair<MutableList<TFAProviderModel>, TFAResolverFactory>)
                MainViewModel.UI_TRIGGER_SHOW_TFA_PROVIDER_SELECTION_FOR_VERIFICATION ->
                    onTFAVerificationProviderSelection(dataPair.second as Pair<MutableList<TFAProviderModel>, TFAResolverFactory>)
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)

        // Reference dynamic item views in order to apply visibility logic.
        val accountItem = menu.findItem(R.id.action_account)
        val logoutItem = menu.findItem(R.id.action_logout)
        val isLoggedIn = viewModel!!.isLoggedIn()

        accountItem.isVisible = isLoggedIn
        logoutItem.isVisible = isLoggedIn

        // Show fingerprint FAB if logged in & support is available.
        if (isLoggedIn && GigyaBiometric.getInstance().isAvailable) {
            fingerprint_fab.visible()
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_account -> showAccountDetails()
            R.id.action_clear -> onClear()
            R.id.action_reinit -> reInit()
            R.id.action_logout -> logout()
            R.id.action_toggle_interruptions -> toggleInterruptions()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun toggleInterruptions() {
        val gigya = Gigya.getInstance()
        gigya.handleInterruptions(!gigya.interruptionsEnabled())
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_send_request -> onSendAnonymousRequest()
            R.id.action_login -> onLogin()
            R.id.action_login_with_provider -> onLoginWithProvider()
            R.id.action_add_connection -> onAddConnection()
            R.id.action_remove_connection -> onRemoveConnection()
            R.id.action_register -> onRegister()
            R.id.action_get_account_info -> onGetAccount()
            R.id.action_set_account_info -> onSetAccount()
            R.id.action_verify_login -> onVerifyLogin()
            R.id.action_native_login -> presentNativeLogin()
            R.id.action_show_screen_sets -> showRAAS()
            R.id.action_forgot_password -> onForgotPassword()
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    //endregion

    //region FINGERPRINT EVALUATION

    private
    val gigyaBiometricCallback = object : IGigyaBiometricCallback {
        override fun onBiometricOperationSuccess(action: GigyaBiometric.Action) {
            toast("Biometric authentication success")
            invalidateOptionsMenu()
            runOnUiThread {
                when (action) {
                    GigyaBiometric.Action.OPT_IN -> {
                        fingerprint_lock_fab.setImageResource(R.drawable.ic_lock_open)
                        fingerprint_fab.setImageResource(R.drawable.ic_fingerprint_opt_out)
                        fingerprint_lock_fab.show()
                    }
                    GigyaBiometric.Action.OPT_OUT -> {
                        fingerprint_fab.setImageResource(R.drawable.ic_fingerprint)
                        fingerprint_lock_fab.hide()
                    }
                    GigyaBiometric.Action.LOCK -> {
                        fingerprint_lock_fab.setImageResource(R.drawable.ic_lock_outline)
                    }
                    GigyaBiometric.Action.UNLOCK -> {
                        fingerprint_lock_fab.setImageResource(R.drawable.ic_lock_open)
                    }
                }
            }
        }

        override fun onBiometricOperationFailed(reason: String?) {
            toast("Biometric authentication error: $reason")
        }

        override fun onBiometricOperationCanceled() {
            toast("Biometric operation canceled")
        }

    }

    /**
     * Evaluate if current session state (if available) is handled using GigyaBiometric fingerprint authentication.
     */
    private fun evaluateFingerprintSession() {
        val biometric = GigyaBiometric.getInstance()
        if (!biometric.isAvailable) {
            // Don't show fingerprint fab is biometric is not supported on device.
            return
        }

        if (viewModel!!.isLoggedIn()) {
            fingerprint_fab.show()
        }
        if (biometric.isOptIn) {
            fingerprint_fab.show()
            fingerprint_lock_fab.show()
            fingerprint_fab.setImageResource(R.drawable.ic_fingerprint_opt_out)
        }
        if (biometric.isLocked) {
            fingerprint_fab.show()
            biometric.unlock(
                    GigyaPromptInfo("Unlock session", "Place finger on sensor to continue", ""),
                    gigyaBiometricCallback)
        }
        // Opt-in/out action.
        fingerprint_fab.setOnClickListener {
            if (biometric.isAvailable && !biometric.isLocked) {
                if (biometric.isOptIn) {
                    biometric.optOut(
                            GigyaPromptInfo("Opt-Out requested", "Place finger on sensor to continue", ""),
                            gigyaBiometricCallback)
                } else {
                    biometric.optIn(
                            GigyaPromptInfo("Opt-In requested", "Place finger on sensor to continue", ""),
                            gigyaBiometricCallback)
                }
            } else {
                toast("Biometric is not supported. Inspect logs for reason")
            }
        }
        // Lock/Unlock button.
        fingerprint_lock_fab.setOnClickListener {
            when (biometric.isLocked) {
                true -> {
                    biometric.unlock(
                            GigyaPromptInfo("Unlock session", "Place finger on sensor to continue", ""),
                            gigyaBiometricCallback)
                }
                false -> {
                    biometric.lock(gigyaBiometricCallback)
                }
            }
        }
    }

    //endregion

    /**
     * Log the user out of the system.
     */
    private fun logout() {
        onClear()
        viewModel?.logout()
        invalidateAccountData()
        response_text_view.snackbar(getString(R.string.logged_out))
        fingerprint_fab.hide()
        fingerprint_lock_fab.hide()
    }

    /**
     * Providing the option to re-initialize the SDK with different ApiKey, ApiDomain parameters.
     */
    private fun reInit() {
        val sheet = InputDialog.newInstance(InputDialog.MainInputType.REINIT, this)
        sheet.show(supportFragmentManager, "sheet")
    }

    //region APIS

    /**
     * Show input dialog for registration
     */
    private fun onRegister() {
        val dialog = InputDialog.newInstance(InputDialog.MainInputType.REGISTER, this)
        dialog.show(supportFragmentManager, "onRegister")
    }

    /**
     * Show input dialog for login.
     */
    private fun onLogin() {
        val dialog = InputDialog.newInstance(InputDialog.MainInputType.LOGIN, this)
        dialog.show(supportFragmentManager, "onLogin")
    }

    /**
     * Show input dialog for social provider input.
     */
    private fun onLoginWithProvider() {
        val dialog = InputDialog.newInstance(InputDialog.MainInputType.LOGIN_WITH_PROVIDER, this)
        dialog.show(supportFragmentManager, "onLoginWithProvider")
    }

    /**
     * Show input dialog for add connection.
     */
    private fun onAddConnection() {
        val dialog = InputDialog.newInstance(InputDialog.MainInputType.ADD_CONNECTION, this)
        dialog.show(supportFragmentManager, "onAddConnection")
    }

    /**
     * Show input dialog for add remove connection.
     */
    private fun onRemoveConnection() {
        val dialog = InputDialog.newInstance(InputDialog.MainInputType.REMOVE_CONNECTION, this)
        dialog.show(supportFragmentManager, "onRemoveConnection")
    }

    /**
     * Show input dialog for anonymous request.
     */
    private fun onSendAnonymousRequest() {
        val dialog = InputDialog.newInstance(InputDialog.MainInputType.ANONYMOUS, this)
        dialog.show(supportFragmentManager, "onSendAnonymousRequest")
    }

    /**
     * Show conflicting accounts dialog.
     */
    private fun onConflictingAccounts(conflictingAccount: ConflictingAccounts) {
        val loginID = conflictingAccount.loginID
        val providers = conflictingAccount.loginProviders
        val dialog = ConflictingAccountsDialog.newInstance(loginID, providers)
        dialog.show(supportFragmentManager, "onConflictingAccounts")
    }

    /**
     * Show TFA provider selection fragment (Fragment is included in the gigya-tfa library).
     */
    private fun onTFARegistrationProviderSelection(dataPair: Pair<MutableList<TFAProviderModel>, TFAResolverFactory>) {
        val dialog = TFAProviderSelectionFragment.newInstance(dataPair.first.map { it.name } as ArrayList<String>)
        dialog.setSelectionCallback(object : TFAProviderSelectionFragment.SelectionCallback {
            override fun onProviderSelected(selectedProvider: String?) {

            }

            override fun onDismiss() {
                // Dismiss the progress bar. Notice that the TFA flow is broken.
                onLoadingDone()
            }

        })
        dialog.show(supportFragmentManager, "onTFARegistrationProviderSelection")
    }

    /**
     * Show TFA provider selection fragment (Fragment is included in the gigya-tfa library).
     */
    private fun onTFAVerificationProviderSelection(dataPair: Pair<MutableList<TFAProviderModel>, TFAResolverFactory>) {
        val dialog = TFAProviderSelectionFragment.newInstance(dataPair.first.map { it.name } as ArrayList<String>)
        dialog.setSelectionCallback(object : TFAProviderSelectionFragment.SelectionCallback {
            override fun onProviderSelected(selectedProvider: String?) {

            }

            override fun onDismiss() {
                // Dismiss the progress bar. Notice that the TFA flow is broken.
                onLoadingDone()
            }

        })
        dialog.show(supportFragmentManager, "onTFAVerificationProviderSelection")
    }

    /**
     * Request myAccountLiveData instance update.
     */
    private fun onGetAccount() {
        if (!viewModel!!.isLoggedIn()) {
            response_text_view.snackbar(getString(R.string.not_logged_in))
            return
        }
        onLoading()
        viewModel?.getAccount(
                success = { json ->
                    onJsonResult(json)
                },
                error = { possibleError ->
                    possibleError?.let { error -> onError(error) }
                }
        )
    }

    /**
     * Set myAccountLiveData requested from navigation menu.
     * Will show dialog to remind client to make sure all fields he is requesting to update are marked as "clientModify".
     */
    private fun onSetAccount() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title).setTitle("Attention!").setMessage("Make sure all updated fields are marked as \"clientModify\"");
        builder.setPositiveButton(getString(android.R.string.ok)) { dialog, _ ->
            if (viewModel?.okayToRequestSetAccount()!!) {
                val sheet = InputDialog.newInstance(InputDialog.MainInputType.SET_ACCOUNT_INFO, this)
                sheet.show(supportFragmentManager, "sheet")
            } else {
                response_text_view.snackbar(getString(R.string.account_not_available))
            }
            dialog.dismiss()
        }
        builder.setNegativeButton(getString(android.R.string.cancel)) { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun onVerifyLogin() {
        if (viewModel!!.isLoggedIn()) {
            viewModel?.verifyLogin(
                    success = { json ->
                        response_text_view.snackbar("Login verified")
                        onJsonResult(json)
                    },
                    error = { possibleError ->
                        possibleError?.let { error -> onError(error) }
                    }
            )
        } else {
            response_text_view.snackbar("Please login to test api")
        }
    }


    /**
     * Forgot password requested from navigation menu.
     */
    private fun onForgotPassword() {
        if (viewModel!!.isLoggedIn()) {
            viewModel?.forgotPassword(
                    success = {
                        response_text_view.snackbar("Reset password email sent.")
                    },
                    error = { possibleError ->
                        possibleError?.let { error -> onError(error) }
                    }
            )
        } else {
            response_text_view.snackbar("Please login to test api. Current view model setup is dependent on a live myAccountLiveData (can be changed)")
        }
    }

    //endregion

    //region UI PRESENTATION

    /**
     * Native login requested from navigation menu.
     * Present Gigya WebView with requested social login providers.
     */
    private fun presentNativeLogin() {
        viewModel?.socialLoginWith(
                success = { json ->
                    onJsonResult(json)
                },
                onIntermediateLoad = {
                    onLoading()
                },
                error = { possibleError ->
                    possibleError?.let { error -> onError(error) }
                }
                , cancel = { response_text_view.snackbar("Request cancelled") }
        )
    }

    /**
     * Registration as a service requested from navigation menu.
     */
    private fun showRAAS() {
        viewModel?.showScreenSets(
                onLogin = { json ->
                    onJsonResult(json)
                },
                onError = { possibleError ->
                    possibleError?.let {
                        // We cant display an alert on top of an alert.
                    }
                }
        )
    }

    /**
     * Show myAccountLiveData details. Will use ScreenSet.
     */
    private fun showAccountDetails() {
        drawer_layout.closeDrawer(GravityCompat.START)
        viewModel?.showAccountDetails(
                onUpdated = {
                    onClear()
                    response_text_view.snackbar(getString(R.string.account_updated))
                    onGetAccount()
                },
                onCanceled = {
                    response_text_view.snackbar("Operation canceled")
                },
                onError = { possibleError ->
                    possibleError?.let {
                        // We cant display an alert on top of an alert.
                    }
                }
        )
    }

    //endregion

    //region VIEW MODEL INTERFACING

    /**
     * SDK re-initialized. force logout/clear session.
     */
    override fun onReInit() {
        viewModel?.logout()
        onClear()
    }

    override fun onAnonymousInput(input: String) {
        onLoading()
        viewModel?.sendAnonymous(input,
                success = { json -> onJsonResult(json) },
                error = { possibleError ->
                    possibleError?.let { error -> onError(error) }
                }
        )
    }

    override fun onLoginWithProvider(provider: String) {
        onLoading()
        viewModel?.loginWithProvider(provider,
                success = { json -> onJsonResult(json) },
                error = { possibleError ->
                    possibleError?.let { error -> onError(error) }
                },
                cancel = {
                    onCancel()
                })
    }

    override fun onAddConnection(provider: String) {
        onLoading()
        viewModel?.addConnection(provider,
                success = { json -> onJsonResult(json) },
                onIntermediateLoad = {
                    onLoading()
                },
                error = { possibleError ->
                    possibleError?.let { error -> onError(error) }
                },
                cancel = {
                    onCancel()
                })
    }

    override fun onRemoveConnection(provider: String) {
        onLoading()
        viewModel?.removeConnection(provider,
                success = { json ->
                    onJsonResult(json)
                    response_text_view.snackbar("Connection removed")
                },
                error = { possibleError ->
                    possibleError?.let { error -> onError(error) }
                }
        )
    }

    override fun onLoginWith(username: String, password: String) {
        onLoading()
        viewModel?.login(username, password,
                success = { json -> onJsonResult(json) },
                error = { possibleError ->
                    possibleError?.let { error -> onError(error) }
                })
    }

    override fun onRegisterWith(username: String, password: String, exp: Int) {
        onLoading()
        viewModel?.register(username, password, exp,
                success = { json -> onJsonResult(json) },
                error = { possibleError ->
                    possibleError?.let { error -> onError(error) }
                })
    }

    /**
     * Trigger a set myAccountLiveData call using the string provided.
     */
    override fun onUpdateAccountWith(comment: String) {
        onLoading()
        viewModel?.setAccount(comment,
                success = { json -> onJsonResult(json) },
                error = { possibleError ->
                    possibleError?.let { error -> onError(error) }
                })
    }

    //endregion

    //region UI HELPERS

    /**
     * Populate JSON result.
     */
    private fun onJsonResult(json: String) {
        response_text_view.text = json
        empty_response_text.gone()
        onLoadingDone()
    }

    /**
     * On Gigya error interfacing. Display error alert.
     */
    private fun onError(error: GigyaError) {
        displayErrorAlert(R.string.rest_error_title, when (error.localizedMessage != null) {
            true -> error.localizedMessage
            false -> "General error"
        })
        onLoadingDone()
    }

    /**
     * Cancelled operation. Display Toast.
     */
    fun onCancel() {
        loader.gone()
        response_text_view.snackbar("Operation canceled")
    }

    /**
     * Show loading state.
     */
    private fun onLoading() {
        loader.visible()
    }

    /**
     * Clear loading state. Invalidate menu.
     */
    private fun onLoadingDone() {
        loader.gone()
        invalidateOptionsMenu()
    }

    /**
     * Call to clear current JSON response presentation.
     */
    private fun onClear() {
        loader.gone()
        response_text_view.text = ""
        empty_response_text.visible()
        invalidateOptionsMenu()
    }

    //endregion

    //region ACCOUNT INFO BINDING

    private
    val accountObserver: Observer<MyAccount> = Observer { myAccount ->
        val fullName = myAccount?.profile?.firstName + " " + myAccount?.profile?.lastName
        nav_title?.text = fullName
        nav_subtitle?.text = myAccount?.profile?.email
        nav_image?.loadRoundImageWith(myAccount?.profile?.thumbnailURL, R.drawable.side_nav_bar)
        invalidateOptionsMenu()
    }

    private fun registerAccountUpdates() {
        viewModel?.myAccountLiveData?.observe(this, accountObserver)
    }

    private fun unregisterAccountUpdates() {
        viewModel?.myAccountLiveData?.removeObserver(accountObserver)
    }

    /**
     * Invalidate navigation header views.
     */
    private fun invalidateAccountData() {
        nav_title?.text = getString(R.string.nav_header_title)
        nav_subtitle?.text = getString(R.string.nav_header_subtitle)
        nav_image?.setImageResource(R.mipmap.ic_launcher_round)
    }

    //endregion

}
