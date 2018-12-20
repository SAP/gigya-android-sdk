package com.gigya.android.sample.ui

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.gigya.android.sample.extras.displayErrorAlert
import com.gigya.android.sample.extras.gone
import com.gigya.android.sample.extras.loadRoundImageWith
import com.gigya.android.sample.extras.visible
import com.gigya.android.sample.model.MyAccount
import com.gigya.android.sdk.Gigya
import com.gigya.android.sdk.model.GigyaAccount
import com.gigya.android.sdk.network.GigyaError
import com.gigya.sample.R
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.nav_header_main.*
import org.jetbrains.anko.design.snackbar

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, MainInputSheet.IApiResultCallback {

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
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)

        // Reference dynamic item views in oreder to apply visibility logic.
        val custom = menu.findItem(R.id.action_custom_scheme)
        val basic = menu.findItem(R.id.action_gigya_scheme)
        val accountItem = menu.findItem(R.id.action_account)

        custom.isVisible = viewModel?.exampleSetup == MainViewModel.SetupExample.BASIC
        basic.isVisible = viewModel?.exampleSetup == MainViewModel.SetupExample.CUSTOM_SCHEME
        accountItem.isVisible = Gigya.getInstance().isLoggedIn

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_custom_scheme -> {
                // Change the scheme to a custom account object.
                viewModel?.exampleSetup = MainViewModel.SetupExample.CUSTOM_SCHEME
                Gigya.getInstance().setAccountScheme(MyAccount::class.java)
                onClear()
                invalidateOptionsMenu()
            }
            R.id.action_gigya_scheme -> {
                // Change to scheme to the default GigyaAccount scheme.
                viewModel?.exampleSetup = MainViewModel.SetupExample.BASIC
                Gigya.getInstance().setAccountScheme(GigyaAccount::class.java)
                onClear()
                invalidateOptionsMenu()
            }
            R.id.action_clear -> {
                onClear()
            }
            // Logout User.
            R.id.action_logout -> {
                onClear()
                viewModel?.logout()
                invalidateAccountData()
                response_text_view.snackbar(getString(R.string.logged_out))
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.api_anonymus -> {
                onSendAnonymousRequest(); }
            R.id.api_login -> {
                onLogin()
            }
            R.id.api_register -> {
                onRegister()
            }
            R.id.api_get_account_info -> {
                onGetAccount()
            }
            R.id.api_set_account_info -> {
                onSetAccount()
            }
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    //endregion

    //region APIs

    private fun onRegister() {
        val sheet = MainInputSheet.newInstance(MainInputSheet.MainInputType.REGISTER, this)
        sheet.show(supportFragmentManager, "sheet")
    }

    private fun onLogin() {
        val sheet = MainInputSheet.newInstance(MainInputSheet.MainInputType.LOGIN, this)
        sheet.show(supportFragmentManager, "sheet")
    }

    private fun onSendAnonymousRequest() {
        val sheet = MainInputSheet.newInstance(MainInputSheet.MainInputType.ANONYMOUS, this)
        sheet.show(supportFragmentManager, "sheet")
    }

    private fun onGetAccount() {
        if (Gigya.getInstance().isLoggedIn) {
            onLoading()
            viewModel?.getAccount(
                    success = { json ->
                        onJsonResult(json)
                        onAccountDataAvailable()
                    },
                    error = { possibleError ->
                        possibleError?.let { error -> onError(error) }
                    }
            )
        } else {
            response_text_view.snackbar(getString(R.string.not_logged_in))
        }
    }

    private fun onSetAccount() {
        if (viewModel?.okayToRequestSetAccount()!!) {
            val sheet = MainInputSheet.newInstance(MainInputSheet.MainInputType.SET_ACCOUNT_INFO, this)
            sheet.show(supportFragmentManager, "sheet")
        } else {
            response_text_view.snackbar(getString(R.string.account_not_available))
        }
    }

    //endregion

    //region UI presentation

    /**
     * On json result (response) interfacing,
     */
    override fun onJsonResult(json: String) {
        response_text_view.text = json
        empty_response_text.gone()
        onLoadingDone()
    }


    /**
     * On Gigya error interfacing. Display error alert.
     */
    override fun onError(error: GigyaError) {
        displayErrorAlert(R.string.rest_error_title, error.localizedMessage)
        onLoadingDone()
    }

    /**
     * Show loading state.
     */
    override fun onLoading() {
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

    /**
     * Bind account data to navigation header views.
     */
    private fun onAccountDataAvailable() {
        viewModel?.getAccountName()?.let { name ->
            nav_title.text = name
        }
        viewModel?.getAccountEmail()?.let { email ->
            nav_subtitle.text = email
        }
        nav_subtitle.text = viewModel?.getAccountEmail()
        nav_image.loadRoundImageWith(viewModel?.getAccountProfileImage(), R.drawable.side_nav_bar)
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
