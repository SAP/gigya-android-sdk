package com.gigya.android.sample.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.design.widget.NavigationView
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
import com.gigya.android.sample.ui.dialog.ConflictingAccountsDialog
import com.gigya.android.sample.ui.dialog.InputDialog
import com.gigya.android.sample.ui.dialog.TFADialog
import com.gigya.android.sdk.Gigya
import com.gigya.android.sdk.api.account.GetConflictingAccountApi
import com.gigya.android.sdk.api.account.RegisterApi
import com.gigya.android.sdk.login.provider.FacebookLoginProvider
import com.gigya.android.sdk.network.GigyaError
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.nav_header_main.*
import org.jetbrains.anko.design.snackbar

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

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    //region Drawer setup

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
                MainViewModel.UI_TRIGGER_SHOW_TFA_REGISTRATION -> showTFARegistrationDialog(dataPair.second as ArrayList<String>)
                MainViewModel.UI_TRIGGER_SHOW_TFA_VERIFICATION -> showTFAVerificationDialog(dataPair.second as ArrayList<String>)
                MainViewModel.UI_TRIGGER_SHOW_TFA_CODE_SENT -> onTFAVerificationCodeSent()
                MainViewModel.UI_TRIGGER_SHOW_CONFLICTING_ACCOUNTS -> onConflictingAccounts(dataPair.second as GetConflictingAccountApi.ConflictingAccount)
            }
        })

        observeAccountUpdates()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)

        // Reference dynamic item views in order to apply visibility logic.
        val accountItem = menu.findItem(R.id.action_account)
        val logoutItem = menu.findItem(R.id.action_logout);
        val facebookPermissionsUpdateItem = menu.findItem(R.id.fb_permission_update)
        val isLoggedIn = Gigya.getInstance().isLoggedIn
        accountItem.isVisible = isLoggedIn
        logoutItem.isVisible = isLoggedIn

        // Check for facebook login
        val loginProvider = Gigya.getInstance().currentProvider
        facebookPermissionsUpdateItem.isVisible = loginProvider != null && Gigya.getInstance().isLoggedIn && loginProvider is FacebookLoginProvider

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_account -> showAccountDetails()
            R.id.action_clear -> onClear()
            R.id.action_reinit -> reInit()
            R.id.action_reset_TFA -> resetTFA()
            R.id.action_logout -> logout()
            R.id.fb_permission_update -> facebookPermissionUpdate()
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
            R.id.action_native_login -> presentNativeLogin()
            R.id.action_raas -> showRAAS()
            R.id.action_comments -> showComments()
            R.id.api_forgot_password -> onForgotPassword()
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    //endregion

    private fun resetTFA() {
        viewModel?.resetTFA(
                success = {
                    response_text_view.snackbar("TFA Method reset")
                },
                error = { possibleError ->
                    possibleError?.let { error -> onError(error) }
                }
        )
    }

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

    /**
     * Request Facebook provider permission update. Using this entry will require you the set up your Facebook application
     * correctly and pass the Facebook's review in order to add & approve additional permissions.
     */
    private fun facebookPermissionUpdate() {
        viewModel?.requestFacebookPermissionUpdate(
                granted = { response_text_view.snackbar("Permission granted") },
                fail = { why -> response_text_view.snackbar(why) },
                cancel = { response_text_view.snackbar("Request cancelled") }
        )
    }

    //region APIs

    private fun onRegister() {
        val dialog = InputDialog.newInstance(InputDialog.MainInputType.REGISTER, this)
        dialog.show(supportFragmentManager, "onRegister")
    }

    private fun onLogin() {
        val dialog = InputDialog.newInstance(InputDialog.MainInputType.LOGIN, this)
        dialog.show(supportFragmentManager, "onLogin")
    }

    private fun onLoginWithProvider() {
        val dialog = InputDialog.newInstance(InputDialog.MainInputType.LOGIN_WITH_PROVIDER, this)
        dialog.show(supportFragmentManager, "onLoginWithProvider")
    }

    private fun onSendAnonymousRequest() {
        val dialog = InputDialog.newInstance(InputDialog.MainInputType.ANONYMOUS, this)
        dialog.show(supportFragmentManager, "onSendAnonymousRequest")
    }

    private fun showTFARegistrationDialog(providers: ArrayList<String>) {
        val dialog = TFADialog.newInstance("registration", providers)
        dialog.show(supportFragmentManager, "showTFARegistrationDialog")
    }

    private fun showTFAVerificationDialog(providers: ArrayList<String>) {
        val dialog = TFADialog.newInstance("verification", providers)
        dialog.show(supportFragmentManager, "showTFAVerificationDialog")
    }

    private fun onTFAVerificationCodeSent() {
        response_text_view.snackbar("Verification code sent")
    }

    private fun onConflictingAccounts(conflictingAccount: GetConflictingAccountApi.ConflictingAccount) {
        val loginID = conflictingAccount.loginID
        val providers = conflictingAccount.loginProviders
        val dialog = ConflictingAccountsDialog.newInstance(loginID, providers)
        dialog.show(supportFragmentManager, "onConflictingAccounts")
    }

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

    //region UI presentation

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

    private fun showAccountDetails() {
        drawer_layout.closeDrawer(GravityCompat.START)
        viewModel?.showAccountDetails(
                onUpdated = {
                    onClear()
                    response_text_view.snackbar(getString(R.string.account_updated))
                    onGetAccount()
                },
                onCancelled = {
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

    //region ViewModel interfacing

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

    override fun onRegisterWith(username: String, password: String, policy: RegisterApi.RegisterPolicy) {
        onLoading()
        viewModel?.register(username, password, policy,
                success = { json -> onJsonResult(json) },
                error = { possibleError ->
                    possibleError?.let { error -> onError(error) }
                })
    }

    override fun onUpdateAccountWith(comment: String) {
        onLoading()
        viewModel?.setAccount(comment,
                success = { json -> onJsonResult(json) },
                error = { possibleError ->
                    possibleError?.let { error -> onError(error) }
                })
    }

    //endregion

    //region UI helpers

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
    private fun onCancel() {
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
        response_text_view.text = ""
        empty_response_text.visible()
        invalidateOptionsMenu()
    }

    //endregion

    //region Account info binding

    private fun observeAccountUpdates() {
        viewModel?.account?.observe(this, Observer { myAccount ->
            val fullName = myAccount?.profile?.firstName + " " + myAccount?.profile?.lastName
            nav_title.text = fullName

            nav_subtitle.text = myAccount?.profile?.email

            nav_image.loadRoundImageWith(myAccount?.profile?.thumbnailURL, R.drawable.side_nav_bar)
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
