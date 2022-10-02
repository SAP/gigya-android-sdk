package com.gigya.android.sample.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.gigya.android.sample.R
import com.gigya.android.sample.extras.displayErrorAlert
import com.gigya.android.sample.extras.gone
import com.gigya.android.sample.extras.loadRoundImageWith
import com.gigya.android.sample.extras.visible
import com.gigya.android.sample.model.MyAccount
import com.gigya.android.sample.ui.fragment.*
import com.gigya.android.sdk.Gigya
import com.gigya.android.sdk.GigyaDefinitions
import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.auth.GigyaAuth
import com.gigya.android.sdk.biometric.GigyaBiometric
import com.gigya.android.sdk.biometric.GigyaPromptInfo
import com.gigya.android.sdk.biometric.IGigyaBiometricCallback
import com.gigya.android.sdk.interruption.link.models.ConflictingAccounts
import com.gigya.android.sdk.interruption.tfa.TFAResolverFactory
import com.gigya.android.sdk.interruption.tfa.models.TFAProviderModel
import com.gigya.android.sdk.network.GigyaError
import com.gigya.android.sdk.nss.GigyaNss
import com.gigya.android.sdk.nss.NssEvents
import com.gigya.android.sdk.nss.bloc.events.*
import com.gigya.android.sdk.push.IGigyaPushCustomizer
import com.gigya.android.sdk.session.SessionStateObserver
import com.gigya.android.sdk.tfa.GigyaTFA
import com.gigya.android.sdk.tfa.ui.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import org.json.JSONObject
import java.util.*


class MainActivity : AppCompatActivity(),
        NavigationView.OnNavigationItemSelectedListener,
        InputDialog.IApiResultCallback {

    private var viewModel: MainViewModel? = null

    private var drawerLayout: DrawerLayout? = null
    private var navigationView: NavigationView? = null
    private var toolbar: Toolbar? = null
    private var fingerprintFab: FloatingActionButton? = null
    private var fingerprintLockFab: FloatingActionButton? = null
    private var responseTextView: TextView? = null
    private var uidTextView: TextView? = null
    private var loader: View? = null
    private var emptyResponseText: TextView? = null
    private var navTitle: TextView? = null
    private var navSubtitle: TextView? = null
    private var navImage: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        title = "Gigya SDK sample"
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        findIds()

        initDrawer()

        GigyaTFA.getInstance().setPushCustomizer(object : IGigyaPushCustomizer {

            override fun getCustomActionActivity(): Class<*> = BiometricPushTFAActivity::class.java

            override fun getDenyActionIcon(): Int = 0

            override fun getSmallIcon(): Int = android.R.drawable.ic_dialog_info

            override fun getApproveActionIcon(): Int = 0

        })
    }

    private fun findIds() {
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        fingerprintFab = findViewById(R.id.fingerprint_fab)
        fingerprintLockFab = findViewById(R.id.fingerprint_lock_fab)
        responseTextView = findViewById(R.id.response_text_view)
        uidTextView = findViewById(R.id.uid_text_view)
        loader = findViewById(R.id.loader)
        emptyResponseText = findViewById(R.id.empty_response_text)
        navTitle = findViewById(R.id.nav_title)
        navSubtitle = findViewById(R.id.nav_subtitle)
        navImage = findViewById(R.id.nav_image)
    }

    override fun onStart() {
        super.onStart()
        // Register for myAccountLiveData info updates.
        registerAccountUpdates()

        Gigya.getInstance().registerSessionVerificationObserver(verificationObserver)
        Gigya.getInstance().registerSessionExpirationObserver(expirationObserver)


        /* Check if this device is opt-in to use push TFA and prompt if notifications are turned off */
        GigyaTFA.getInstance().registerForRemoteNotifications(this)

        /* Check if this device is registered to use push authentication and prompt if notifications are turned off */
        GigyaAuth.getInstance().registerForPushNotifications(this)

    }

    private val verificationObserver = SessionStateObserver { data ->
        data?.let {
            if (data is JSONObject) {
                println(it.toString())
            }
        }
        runOnUiThread {
            displayErrorAlert("Session state alert", "session verification failed")
            onClear()
        }
    }

    private val expirationObserver = SessionStateObserver {
        runOnUiThread {
            displayErrorAlert("Session state alert", "session expired")
            onClear()
        }
    }

    override fun onResume() {
        super.onResume()

        val filter = IntentFilter()
        filter.addAction(GigyaDefinitions.Broadcasts.INTENT_ACTION_SESSION_EXPIRED)
        filter.addAction(GigyaDefinitions.Broadcasts.INTENT_ACTION_SESSION_INVALID)
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).registerReceiver(sessionLifecycleReceiver,
                filter)

        // Evaluate fingerprint session.
        evaluateFingerprintSession()

        /* If we are already logged in - get myAccountLiveData info and update relevant myAccountLiveData UI (drawer header). */
        if (viewModel!!.isLoggedIn()) {
            onGetAccount()
        } else {
            onClear()
        }
    }

    override fun onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(sessionLifecycleReceiver)

        val biometric = GigyaBiometric.getInstance()
        if (biometric.isLocked) {
            onClear()
        }
        super.onPause()
    }

    override fun onStop() {
        unregisterAccountUpdates()

        Gigya.getInstance().unregisterSessionVerificationObserver(verificationObserver)
        Gigya.getInstance().unregisterSessionExpirationObserver(expirationObserver)

        super.onStop()
    }

    override fun onDestroy() {
        GigyaLogger.exportSmartLog(this)
        super.onDestroy()
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
                        GigyaDefinitions.Broadcasts.INTENT_ACTION_SESSION_INVALID -> {
                            "Your session is invalid"
                        }
                        else -> ""
                    }
                    if (intent_action == GigyaDefinitions.Broadcasts.INTENT_ACTION_SESSION_INVALID) {
                        // We can fetch the "regToken" value for additional flows.
                        intent.extras?.let { bundle ->
                            {
                                if (bundle.containsKey("regToken")) {
                                    val regToken: String? = intent.getStringExtra("regToken")
                                    GigyaLogger.debug("MainActivity", "regToken = $regToken")
                                }
                            }
                        }
                    }
//                    runOnUiThread {
//                        displayErrorAlert("Alert", message)
//                        onClear()
//                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        if (drawerLayout!!.isDrawerOpen(GravityCompat.START)) {
            drawerLayout!!.closeDrawer(GravityCompat.START)
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
                this, drawerLayout!!, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawerLayout!!.addDrawerListener(toggle)
        toggle.syncState()

        navigationView!!.setNavigationItemSelectedListener(this)

        /* Setup drawer navigation header click listener. */
        navigationView!!.getHeaderView(0)?.setOnClickListener {
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
                MainViewModel.UI_TRIGGER_SHOW_PENDING_REGISTRATION_UI -> onPendingRegistrationUI()

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
            fingerprintFab!!.visible()
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
            R.id.action_is_session_valid -> isSessionValid()
            R.id.action_send_request -> onSendAnonymousRequest()
            R.id.action_login -> onLogin()
            R.id.action_login_with_provider -> onLoginWithProvider()
            R.id.action_add_connection -> onAddConnection()
            R.id.action_remove_connection -> onRemoveConnection()
            R.id.action_register -> onRegister()
            R.id.action_get_account_info -> onGetAccount()
            R.id.action_get_account_and_invalidate_cache -> getAccountAndInvalidateCache()
            R.id.action_set_account_info -> onSetAccount()
            R.id.action_verify_login -> onVerifyLogin()
            R.id.action_native_login -> presentNativeLogin()
            R.id.action_otp_login -> otpLogin()
            R.id.action_show_screen_sets -> showRAAS()
            R.id.action_forgot_password -> onForgotPassword()
            R.id.action_push_tfa_opt_in -> optInForPushTFA()
            R.id.action_push_auth_register -> registerForPushAuthentication()
            R.id.action_web_bridge_test -> startActivity(Intent(this, WebBridgeTestActivity::class.java))
            R.id.action_show_native_screen_sets -> showNativeScreenSets()
            R.id.action_sso_login -> ssoLogin()
        }
        drawerLayout!!.closeDrawer(GravityCompat.START)
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
                        fingerprintLockFab!!.setImageResource(R.drawable.ic_lock_open)
                        fingerprintFab!!.setImageResource(R.drawable.ic_fingerprint_opt_out)
                        fingerprintLockFab!!.show()
                    }
                    GigyaBiometric.Action.OPT_OUT -> {
                        fingerprintFab!!.setImageResource(R.drawable.ic_fingerprint)
                        fingerprintLockFab!!.hide()
                    }
                    GigyaBiometric.Action.LOCK -> {
                        fingerprintLockFab!!.setImageResource(R.drawable.ic_lock_outline)
                    }
                    GigyaBiometric.Action.UNLOCK -> {
                        fingerprintLockFab!!.setImageResource(R.drawable.ic_lock_open)
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
            fingerprintFab!!.show()
        }
        if (biometric.isOptIn) {
            fingerprintFab!!.show()
            fingerprintLockFab!!.show()
            fingerprintFab!!.setImageResource(R.drawable.ic_fingerprint_opt_out)
        }
        if (biometric.isLocked) {
            fingerprintFab!!.show()
            biometric.unlock(
                    this,
                    GigyaPromptInfo("Unlock session", "Place finger on sensor to continue", ""),
                    gigyaBiometricCallback)
        }
        // Opt-in/out action.
        fingerprintFab!!.setOnClickListener {
            if (biometric.isAvailable && !biometric.isLocked) {
                if (biometric.isOptIn) {
                    biometric.optOut(
                            this,
                            GigyaPromptInfo("Opt-Out requested", "Place finger on sensor to continue", ""),
                            gigyaBiometricCallback)
                } else {
                    biometric.optIn(
                            this,
                            GigyaPromptInfo("Opt-In requested", "Place finger on sensor to continue", ""),
                            gigyaBiometricCallback)
                }
            } else {
                toast("Biometric is not supported. Inspect logs for reason")
            }
        }
        // Lock/Unlock button.
        fingerprintLockFab!!.setOnClickListener {
            when (biometric.isLocked) {
                true -> {
                    biometric.unlock(
                            this,
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
        toast(getString(R.string.logged_out))
        fingerprintFab!!.hide()
        fingerprintLockFab!!.hide()
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
        val providerDialog = TFAProviderSelectionFragment.newInstance(dataPair.first.map { it.name } as ArrayList<String>)
        providerDialog.setRoundedCorners(true)
        providerDialog.setSelectionCallback(object : TFAProviderSelectionFragment.SelectionCallback {
            override fun onProviderSelected(selectedProvider: String) {
                when (selectedProvider) {
                    com.gigya.android.sdk.tfa.GigyaDefinitions.TFAProvider.PHONE,
                    com.gigya.android.sdk.tfa.GigyaDefinitions.TFAProvider.LIVELINK -> {
                        // Resolving Phone TFA registration.
                        val phoneDialog = CustomTFAPhoneRegistrationFragment.newInstance(selectedProvider, "en")
                        phoneDialog.setRoundedCorners(true)
                        phoneDialog.setResolverFactory(dataPair.second)
                        phoneDialog.setSelectionCallback(object : BaseTFAFragment.SelectionCallback {
                            override fun onDismiss() {
                                // Dismiss the progress bar. Notice that the TFA flow is broken.
                                onLoadingDone()
                            }

                            override fun onResolved() {
                                // This callback is used to notify that the flow has been resolved.
                                // Once resolved the initial onSuccess callback will be called.
                            }

                            override fun onError(error: GigyaError?) {
                                onLoadingDone()
                                displayErrorAlert("TFA flow error", error?.localizedMessage!!)
                            }

                        })
                        phoneDialog.show(supportFragmentManager, "TFAPhoneRegistrationFragment")
                    }
                    com.gigya.android.sdk.tfa.GigyaDefinitions.TFAProvider.TOTP -> {
                        val totpDialog = TFATOTPRegistrationFragment.newInstance()
                        totpDialog.setRoundedCorners(true)
                        totpDialog.setResolverFactory(dataPair.second)
                        totpDialog.setSelectionCallback(object : BaseTFAFragment.SelectionCallback {
                            override fun onDismiss() {
                                // Dismiss the progress bar. Notice that the TFA flow is broken.
                                onLoadingDone()
                            }

                            override fun onResolved() {
                                // This callback is used to notify that the flow has been resolved.
                                // Once resolved the initial onSuccess callback will be called.
                            }

                            override fun onError(error: GigyaError?) {
                                onLoadingDone()
                                displayErrorAlert("TFA flow error", error?.localizedMessage!!)
                            }

                        })
                        totpDialog.show(supportFragmentManager, "TFATOTPRegistrationFragment")
                    }

                }
            }

            override fun onDismiss() {
                // Dismiss the progress bar. Notice that the TFA flow is broken.
                onLoadingDone()
            }

        })
        providerDialog.show(supportFragmentManager, "onTFARegistrationProviderSelection")
    }

    /**
     * Show TFA provider selection fragment (Fragment is included in the gigya-tfa library).
     */
    private fun onTFAVerificationProviderSelection(dataPair: Pair<MutableList<TFAProviderModel>, TFAResolverFactory>) {
        val providerDialog = TFAProviderSelectionFragment.newInstance(dataPair.first.map { it.name } as ArrayList<String>)
        providerDialog.setRoundedCorners(true)
        providerDialog.setSelectionCallback(object : TFAProviderSelectionFragment.SelectionCallback {
            override fun onProviderSelected(selectedProvider: String) {
                when (selectedProvider) {
                    com.gigya.android.sdk.tfa.GigyaDefinitions.TFAProvider.PHONE,
                    com.gigya.android.sdk.tfa.GigyaDefinitions.TFAProvider.LIVELINK -> {
                        val phoneDialog = TFAPhoneVerificationFragment.newInstance(selectedProvider, "en")
                        phoneDialog.setRoundedCorners(true)
                        phoneDialog.setResolverFactory(dataPair.second)
                        phoneDialog.setSelectionCallback(object : BaseTFAFragment.SelectionCallback {
                            override fun onDismiss() {
                                // Dismiss the progress bar. Notice that the TFA flow is broken.
                                onLoadingDone()
                            }

                            override fun onResolved() {
                                // This callback is used to notify that the flow has been resolved.
                                // Once resolved the initial onSuccess callback will be called.
                            }

                            override fun onError(error: GigyaError?) {
                                onLoadingDone()
                                displayErrorAlert("TFA flow error", error?.localizedMessage!!)
                            }
                        })
                        phoneDialog.show(supportFragmentManager, "TFAPhoneVerificationFragment")
                    }
                    com.gigya.android.sdk.tfa.GigyaDefinitions.TFAProvider.EMAIL -> {
                        val emailDialog = TFAEmailVerificationFragment.newInstance("en")
                        emailDialog.setRoundedCorners(true)
                        emailDialog.setResolverFactory(dataPair.second)
                        emailDialog.setSelectionCallback(object : BaseTFAFragment.SelectionCallback {
                            override fun onDismiss() {
                                // Dismiss the progress bar. Notice that the TFA flow is broken.
                                onLoadingDone()
                            }

                            override fun onResolved() {
                                // This callback is used to notify that the flow has been resolved.
                                // Once resolved the initial onSuccess callback will be called.
                            }

                            override fun onError(error: GigyaError?) {
                                onLoadingDone()
                                displayErrorAlert("TFA flow error", error?.localizedMessage!!)
                            }
                        })
                        emailDialog.show(supportFragmentManager, "TFAEmailVerificationFragment")
                    }
                    com.gigya.android.sdk.tfa.GigyaDefinitions.TFAProvider.TOTP -> {
                        val totpDialog = TFATOTPVerificationFragment.newInstance()
                        totpDialog.setRoundedCorners(true)
                        totpDialog.setResolverFactory(dataPair.second)
                        totpDialog.setSelectionCallback(object : BaseTFAFragment.SelectionCallback {
                            override fun onDismiss() {
                                // Dismiss the progress bar. Notice that the TFA flow is broken.
                                onLoadingDone()
                            }

                            override fun onResolved() {
                                // This callback is used to notify that the flow has been resolved.
                                // Once resolved the initial onSuccess callback will be called.
                            }

                            override fun onError(error: GigyaError?) {
                                onLoadingDone()
                                displayErrorAlert("TFA flow error", error?.localizedMessage!!)
                            }
                        })
                        totpDialog.show(supportFragmentManager, "TFATOTPVerificationFragment")
                    }
                }
            }

            override fun onDismiss() {
                // Dismiss the progress bar. Notice that the TFA flow is broken.
                onLoadingDone()
            }

        })
        providerDialog.show(supportFragmentManager, "onTFAVerificationProviderSelection")
    }

    private fun onPendingRegistrationUI() {
        val sheet = InputDialog.newInstance(InputDialog.MainInputType.PENDING_REGISTRATION, this)
        sheet.show(supportFragmentManager, "set_account_sheet")
    }

    /**
     * Request myAccountLiveData instance update.
     */
    private fun onGetAccount() {
        if (!viewModel!!.isLoggedIn()) {
           toast(getString(R.string.not_logged_in))
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

    private fun getAccountAndInvalidateCache() {
        if (!viewModel!!.isLoggedIn()) {
            toast(getString(R.string.not_logged_in))
            return
        }
        onLoading()
        viewModel?.getAccountAndInvalidateCache(
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
        val sheet = InputDialog.newInstance(InputDialog.MainInputType.SET_ACCOUNT_INFO, this)
        sheet.show(supportFragmentManager, "set_account_sheet")
    }

    private fun onVerifyLogin() {
        if (viewModel!!.isLoggedIn()) {
            viewModel?.verifyLogin(
                    success = { json ->
                        toast("Login verified")
                        onJsonResult(json)
                    },
                    error = { possibleError ->
                        possibleError?.let { error -> onError(error) }
                    }
            )
        } else {
            toast("Please login to test api")
        }
    }


    /**
     * Forgot password requested from navigation menu.
     */
    private fun onForgotPassword() {
        if (viewModel!!.isLoggedIn()) {
            viewModel?.forgotPassword(
                    success = {
                        toast("Reset password email sent.")
                    },
                    error = { possibleError ->
                        possibleError?.let { error -> onError(error) }
                    }
            )
        } else {
            toast("Please login to test api. Current view model setup is dependent on a live myAccountLiveData (can be changed)")
        }
    }

    /**
     * Opt-in to use TFA push. (TFA library required)
     */
    private fun optInForPushTFA() {
        if (viewModel!!.isLoggedIn()) {
            viewModel?.pushTFAOptIn(
                    success = {
                        toast("Approve opt-in notification to complete TFA push registration")
                    },
                    error = { possibleError ->
                        possibleError?.let { error -> onError(error) }
                    }
            )
        } else {
            toast("An active session is required")
        }
    }

    /**
     * Register for push authentication. (Auth library required)
     */
    private fun registerForPushAuthentication() {
        if (viewModel!!.isLoggedIn()) {
            viewModel?.pushAuthRegister(
                    success = {
                        toast("Successfully registered for push authentication")
                    },
                    error = { possibleError ->
                        possibleError?.let { error -> onError(error) }
                    }
            )
        } else {
            toast("An active session is required")
        }
    }

    private fun showNativeScreenSets() {
        GigyaNss.getInstance()
//                .load("DEFAULT")
                .loadFromAssets("gigya-nss-example")
                .initialRoute("register")
                //.lang("es")
                .events(object : NssEvents<MyAccount>() {

                    override fun onError(screenId: String, error: GigyaError) {
                        // Handle nss exception here.
                        GigyaLogger.debug("NSS", "onError")
                    }

                    override fun onCancel() {
                        // Handle cancel event if needed.
                        GigyaLogger.debug("NSS", "onCancel")
                    }

                    override fun onScreenSuccess(screenId: String, action: String, accountObj: MyAccount?) {
                        // Handle login event here if needed.
                        GigyaLogger.debug("NSS", "onSuccess for screen: $screenId and action: $action")
                    }

                })
                .eventsFor("login", object : NssScreenEvents() {

                    override fun screenDidLoad() {
                        GigyaLogger.debug("NssEvents", "screen did load for login")
                    }

                    override fun routeFrom(screen: ScreenRouteFromModel) {
                        GigyaLogger.debug("NssEvents", "routeFrom: from: " + screen.previousRoute())
                        super.routeFrom(screen)
                    }

                    override fun routeTo(screen: ScreenRouteToModel) {
                        GigyaLogger.debug("NssEvents", "routeTo: to: " + screen.nextRoute() + "data: " + screen.screenData().toString())
                        super.routeTo(screen)
                    }

                    override fun submit(screen: ScreenSubmitModel) {
                        GigyaLogger.debug("NssEvents", "submit: data: " + screen.screenData().toString())
                        super.submit(screen)
                    }

                    override fun fieldDidChange(screen: ScreenFieldModel, field: FieldEventModel) {
                        GigyaLogger.debug("NssEvents", "fieldDidChange: field:" + field.id + " oldVal: " + field.oldVal + " newVal: " + field.newVal)
                        super.fieldDidChange(screen, field)
                    }

                })
                .show(this)
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
                }, cancel = { toast("Request cancelled") }
        )
    }

    private fun otpLogin() {
        val dialog = CustomOTPRegistrationFragment.newInstance(object : IOTPResultCallback {

            override fun onOTPResult(json: String) {
                onJsonResult(json)
            }

        })
        dialog.show(supportFragmentManager, "otp_login")
    }

    /**
     * Registration as a service requested from navigation menu.
     */
    private fun showRAAS() {
        viewModel?.showScreenSets(
                onLogin = { json ->
                    onJsonResult(json)
                },
                onCanceled = {
                    toast("Operation canceled")
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
        drawerLayout!!.closeDrawer(GravityCompat.START)
//        if (GigyaNss.getInstance().isSupported) {
//        GigyaNss.getInstance()
//                .loadFromAssets("gigya-nss-example")
//                .initialRoute("account-update")
//                .events(object : NssEvents<MyAccount>() {
//
//                    override fun onError(screenId: String, error: GigyaError) {
//                        // Handle nss exception here.
//                        GigyaLogger.debug("NSS", "onError")
//                    }
//
//                    override fun onCancel() {
//                        // Handle cancel event if needed.
//                        GigyaLogger.debug("NSS", "onCancel")
//                    }
//
//                    override fun onScreenSuccess(screenId: String, action: String, accountObj: MyAccount?) {
//                        // Handle login event here if needed.
//                        GigyaLogger.debug("NSS", "onSuccess for screen: $screenId and action: $action")
//                    }
//
//                })
//                .eventsFor("login", object : NssScreenEvents() {
//
//                })
//                .show(this)
//        } else {
        viewModel?.showAccountDetails(
                onUpdated = {
                    onClear()
                    toast(getString(R.string.account_updated))
                    onGetAccount()
                },
                onCanceled = {
                    toast("Operation canceled")
                },
                onError = { possibleError ->
                    possibleError?.let {
                        // We cant display an alert on top of an alert.
                    }
                }
        )
//        }
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

    private fun isSessionValid() {
        if (!viewModel!!.isLoggedIn()) {
            toast("Active session is required")
            return
        }
        viewModel!!.isSessionValid(
                success = { json -> onJsonResult(json) },
                error = { possibleError ->
                    possibleError?.let { error -> onError(error) }
                },
        )

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
                    toast("Connection removed")
                },
                error = { possibleError ->
                    possibleError?.let { error -> onError(error) }
                }
        )
    }

    override fun onLoginWith(username: String, password: String, exp: Int) {
        onLoading()
        viewModel?.login(username, password, exp,
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

    override fun onUpdateAccountWith(comment: String) {
        onLoading()
        viewModel?.setAccount(comment,
                success = { json -> onJsonResult(json) },
                error = { possibleError ->
                    possibleError?.let { error -> onError(error) }
                })
    }

    override fun onUpdateAccountWith(field: String, value: String, forPendingRegistration: Boolean) {
        onLoading()
        when (forPendingRegistration) {
            false -> {
                viewModel?.setAccount(field, value,
                        success = { json -> onJsonResult(json) },
                        error = { possibleError ->
                            possibleError?.let { error -> onError(error) }
                        }
                )
            }
            true -> viewModel?.onResolvePendingRegistrationWithMissingData(field, value)
        }

    }

    private fun ssoLogin() {
        viewModel?.ssoLogin(
                success = { json ->
                    onJsonResult(json)
                },
                error = { possibleError ->
                    possibleError?.let { error -> onError(error) }
                },
                cancel = { toast("Request cancelled") }
        )
    }

    //endregion

    //region UI HELPERS

    /**
     * Populate JSON result.
     */
    private fun onJsonResult(json: String) {
        responseTextView!!.text = json
        emptyResponseText!!.gone()
        onLoadingDone()

        // Update UI field if available.
    }

    /**
     * On Gigya error interfacing. Display error alert.
     */
    private fun onError(error: GigyaError) {
        displayErrorAlert(R.string.rest_error_title,
                when (error.localizedMessage != null) {
                    true -> error.localizedMessage
                    false -> "General error"
                })
        onLoadingDone()
    }

    /**
     * Cancelled operation. Display Toast.
     */
    fun onCancel() {
        loader!!.gone()
        toast("Operation canceled")
    }

    /**
     * Show loading state.
     */
    private fun onLoading() {
        loader!!.visible()
    }

    /**
     * Clear loading state. Invalidate menu.
     */
    private fun onLoadingDone() {
        loader!!.gone()
        invalidateOptionsMenu()
    }

    /**
     * Call to clear current JSON response presentation.
     */
    private fun onClear() {
        loader!!.gone()
        responseTextView!!.text = ""
        emptyResponseText!!.visible()
        invalidateOptionsMenu()
        fingerprintLockFab!!.hide()
    }

    //endregion

    //region ACCOUNT INFO BINDING

    private
    val accountObserver: Observer<MyAccount> = Observer { myAccount ->
        val fullName = myAccount?.profile?.firstName + " " + myAccount?.profile?.lastName
        navTitle?.text = fullName
        navSubtitle?.text = myAccount?.profile?.email
        navImage?.loadRoundImageWith(myAccount?.profile?.thumbnailURL, R.drawable.side_nav_bar)
        invalidateOptionsMenu()

        myAccount?.uid?.let {
            uidTextView!!.text = it
            uidTextView!!.visible()
        }
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
        navTitle?.text = getString(R.string.nav_header_title)
        navSubtitle?.text = getString(R.string.nav_header_subtitle)
        navImage?.setImageResource(R.mipmap.ic_launcher_round)
        uidTextView!!.text = ""
        uidTextView!!.gone()
    }

    //endregion

    private fun changeLocale(identifier: String) {

        val configuration = resources.configuration
        val newLocale = Locale(identifier)
        Locale.setDefault(newLocale)
        configuration.setLocale(newLocale)
        val context = createConfigurationContext(configuration)
    }

    private fun toast(text: String) {
        val toast = Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT)
        toast.show()
    }
}
