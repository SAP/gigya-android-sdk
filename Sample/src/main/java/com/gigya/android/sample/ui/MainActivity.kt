package com.gigya.android.sample.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.biometrics.BiometricPrompt
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
import com.gigya.android.sample.ui.fragment.BackPressListener
import com.gigya.android.sample.ui.fragment.ConflictingAccountsDialog
import com.gigya.android.sample.ui.fragment.InputDialog
import com.gigya.android.sample.ui.fragment.TFAFragment
import com.gigya.android.sdk.Gigya
import com.gigya.android.sdk.GigyaDefinitions
import com.gigya.android.sdk.biometric.GigyaBiometric
import com.gigya.android.sdk.biometric.IGigyaBiometricCallback
import com.gigya.android.sdk.model.account.ConflictingAccounts
import com.gigya.android.sdk.model.tfa.TFAProvider
import com.gigya.android.sdk.network.GigyaError
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.nav_header_main.*
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.toast


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, InputDialog.IApiResultCallback {

    private var viewModel: MainViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        title = "Gigya SDK sample"

        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        setSupportActionBar(toolbar)

        initDrawer()

        initLockButton()
    }

    private fun initLockButton() {
        finger_print_lock_button.setOnClickListener {
            val biometric = GigyaBiometric.Builder().build()
            biometric.optIn(this, object: IGigyaBiometricCallback() {
                override fun onAuthenticationError(errMsgId: Int, errString: CharSequence?) {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }

                override fun onAuthenticationFailed() {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }

                override fun onAuthenticationHelp(helpCode: Int, helpString: CharSequence?) {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {

                }
            })
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter()
        filter.addAction(GigyaDefinitions.Broadcasts.INTENT_ACTION_SESSION_EXPIRED)
        filter.addAction(GigyaDefinitions.Broadcasts.INTENT_ACTION_SESSION_INVALID)
        LocalBroadcastManager.getInstance(this).registerReceiver(sessionLifecycleReceiver,
                IntentFilter(filter))
    }

    override fun onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(sessionLifecycleReceiver)
        super.onPause()
    }

    private val sessionLifecycleReceiver: BroadcastReceiver = object : BroadcastReceiver() {
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
            if (Gigya.getInstance().isLoggedIn) {
                showAccountDetails()
            }
        }

        /* If we are already logged in - get account info and update relevant account UI (drawer header). */
        if (Gigya.getInstance().isLoggedIn) {
            onGetAccount()
        }

        viewModel?.uiTrigger?.observe(this, Observer { dataPair ->

            @Suppress("UNCHECKED_CAST")
            when (dataPair?.first) {
                MainViewModel.UI_TRIGGER_SHOW_TFA_REGISTRATION -> showTFARegistrationFragment(dataPair.second as ArrayList<TFAProvider>)
                MainViewModel.UI_TRIGGER_SHOW_TFA_VERIFICATION -> showTFAVerificationFragment(dataPair.second as ArrayList<TFAProvider>)
                MainViewModel.UI_TRIGGER_SHOW_TFA_EMAIL_SENT -> toast("Verification email sent")
                MainViewModel.UI_TRIGGER_SHOW_CONFLICTING_ACCOUNTS -> onConflictingAccounts(dataPair.second as ConflictingAccounts)
            }
        })

        observeAccountUpdates()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)

        // Reference dynamic item views in order to apply visibility logic.
        val accountItem = menu.findItem(R.id.action_account)
        val logoutItem = menu.findItem(R.id.action_logout)
        val isLoggedIn = Gigya.getInstance().isLoggedIn
        accountItem.isVisible = isLoggedIn
        logoutItem.isVisible = isLoggedIn
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_account -> showAccountDetails()
            R.id.action_clear -> onClear()
            R.id.action_reinit -> reInit()
            R.id.action_logout -> logout()
            R.id.disable_interruptions -> Gigya.getInstance().handleInterruptions(false)
            R.id.enable_interruptions -> Gigya.getInstance().handleInterruptions(true)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.api_anonymous -> onSendAnonymousRequest()
            R.id.api_login -> onLogin()
            R.id.api_login_with_provider -> onLoginWithProvider()
            R.id.api_register -> onRegister()
            R.id.api_get_account_info -> onGetAccount()
            R.id.api_set_account_info -> onSetAccount()
            R.id.api_verify_login -> onVerifyLogin()
            R.id.action_native_login -> presentNativeLogin()
            R.id.action_raas -> showRAAS()
            R.id.action_comments -> showComments()
            R.id.api_forgot_password -> onForgotPassword()
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
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
     * Show input dialog for anonymous request.
     */
    private fun onSendAnonymousRequest() {
        val dialog = InputDialog.newInstance(InputDialog.MainInputType.ANONYMOUS, this)
        dialog.show(supportFragmentManager, "onSendAnonymousRequest")
    }

    /**
     * Show TFA fragment for registration mode.
     */
    private fun showTFARegistrationFragment(providers: ArrayList<TFAProvider>) {
        val fragment = TFAFragment.newInstance("registration", ArrayList(providers.map { it.name }))
        supportFragmentManager.beginTransaction().add(R.id.frag_container, fragment).addToBackStack(null).commit()
    }

    /**
     * Show TFA fragment for verification mode.
     */
    private fun showTFAVerificationFragment(providers: ArrayList<TFAProvider>) {
        val fragment = TFAFragment.newInstance("verification", ArrayList(providers.map { it.name }))
        supportFragmentManager.beginTransaction().add(R.id.frag_container, fragment).addToBackStack(null).commit()
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
     * Request account instance update.
     */
    private fun onGetAccount() {
        if (!Gigya.getInstance().isLoggedIn) {
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
     * Set account requested from navigation menu.
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
        if (Gigya.getInstance().isLoggedIn) {
            viewModel?.verifyLogin(
                    success = { json ->
                        onJsonResult(json)
                    },
                    error = { possibleError ->
                        possibleError?.let { error -> onError(error) }
                    }
            )
        }
    }


    /**
     * Forgot password requested from navigation menu.
     */
    private fun onForgotPassword() {
        viewModel?.forgotPassword(
                success = {
                    response_text_view.snackbar("Reset password email sent.")
                },
                error = { possibleError ->
                    possibleError?.let { error -> onError(error) }
                }
        )
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
        viewModel?.registrationAsAService(
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
     * Comments requested from navigation menu.
     */
    private fun showComments() {
        viewModel?.showComments(
                onLogin = { json ->
                    onJsonResult(json)
                },
                onLogout = {
                    onClear()
                    response_text_view.snackbar(getString(R.string.logged_out))
                },
                onError = { possibleError ->
                    possibleError?.let {
                        // We cant display an alert on top of an alert.
                    }
                }
        )
    }

    /**
     * Show account details. Will use ScreenSet.
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

    //region VIEWMODEL INTERFACING

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
     * Trigger a set account call using the string provided.
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
        displayErrorAlert(R.string.rest_error_title, error.localizedMessage)
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

    private fun observeAccountUpdates() {
        viewModel?.account?.observe(this, Observer { myAccount ->
            val fullName = myAccount?.profile?.firstName + " " + myAccount?.profile?.lastName
            nav_title.text = fullName

            nav_subtitle.text = myAccount?.profile?.email

            nav_image.loadRoundImageWith(myAccount?.profile?.thumbnailURL, R.drawable.side_nav_bar)

            invalidateOptionsMenu()
        })
    }

    /**
     * Invalidate navigation header views.
     */
    private fun invalidateAccountData() {
        nav_title.text = getString(R.string.nav_header_title)
        nav_subtitle.text = getString(R.string.nav_header_subtitle)
        nav_image.setImageResource(R.mipmap.ic_launcher_round)
    }

    //endregion
}
