package com.gigya.android.sample.ui.fragment

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.gigya.android.sample.R
import com.gigya.android.sample.extras.visible
import com.gigya.android.sample.ui.MainViewModel
import com.gigya.android.sdk.Gigya
import kotlinx.android.synthetic.main.input_anonymous.*
import kotlinx.android.synthetic.main.input_login_register.*
import kotlinx.android.synthetic.main.input_login_with_provider.*
import kotlinx.android.synthetic.main.input_re_init.*
import kotlinx.android.synthetic.main.input_set_account.*
import org.jetbrains.anko.design.snackbar

class InputDialog : DialogFragment() {

    private var viewModel: MainViewModel? = null

    enum class MainInputType {
        ANONYMOUS,
        LOGIN, REGISTER,
        SET_ACCOUNT_INFO,
        REINIT,
        LOGIN_WITH_PROVIDER,
        ADD_CONNECTION,
        REMOVE_CONNECTION,
        PENDING_REGISTRATION,
    }

    interface IApiResultCallback {
        fun onReInit()
        fun onAnonymousInput(input: String)
        fun onLoginWithProvider(provider: String)
        fun onRegisterWith(username: String, password: String, exp: Int)
        fun onLoginWith(username: String, password: String)
        fun onUpdateAccountWith(comment: String)
        fun onUpdateAccountWith(field: String, value: String, forPendingRegistration: Boolean)
        fun onAddConnection(provider: String)
        fun onRemoveConnection(provider: String)
    }

    private var type: MainInputType? = null

    lateinit var resultCallback: IApiResultCallback

    companion object {

        fun newInstance(type: MainInputType, resultCallback: IApiResultCallback): InputDialog {
            val args = Bundle()
            args.putSerializable("type", type)
            val fragment = InputDialog()
            fragment.arguments = args
            fragment.resultCallback = resultCallback
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.type = arguments!!["type"] as MainInputType
        viewModel = activity?.run {
            ViewModelProviders.of(this).get(MainViewModel::class.java)
        } ?: throw Exception("Invalid Activity")
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val layoutParams = dialog?.window?.attributes
        layoutParams?.width = ViewGroup.LayoutParams.MATCH_PARENT
        dialog?.window?.attributes = layoutParams
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val layout = when (type) {
            MainInputType.REINIT -> R.layout.input_re_init
            MainInputType.ANONYMOUS -> R.layout.input_anonymous
            MainInputType.LOGIN, MainInputType.REGISTER -> R.layout.input_login_register
            MainInputType.SET_ACCOUNT_INFO, MainInputType.PENDING_REGISTRATION -> R.layout.input_set_account
            MainInputType.LOGIN_WITH_PROVIDER, MainInputType.ADD_CONNECTION, MainInputType.REMOVE_CONNECTION -> R.layout.input_login_with_provider
            else -> 0
        }
        return inflater.inflate(layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        when (type) {
            MainInputType.REINIT -> setupForReInit()
            MainInputType.ANONYMOUS -> setupForAnonymous()
            MainInputType.REGISTER, MainInputType.LOGIN -> setupForLoginRegister()
            MainInputType.SET_ACCOUNT_INFO -> setupForSetAccountInfo(false)
            MainInputType.PENDING_REGISTRATION -> setupForSetAccountInfo(true)
            MainInputType.LOGIN_WITH_PROVIDER -> setupForLoginWithProvider()
            MainInputType.ADD_CONNECTION, MainInputType.REMOVE_CONNECTION -> setupForAddOrRemoveConnection(type!!)
        }
    }

    /**
     * Setup input dialog for SDK re-initialization option.
     */
    private fun setupForReInit() {
        re_init_apply_button.setOnClickListener {
            val apiDomainId: Int = api_domain_selection_group.checkedRadioButtonId
            val apiDomainString: String = when (apiDomainId) {
                R.id.domain_us1 -> "us1.gigya.com"
                R.id.domain_eu1 -> "eu1.gigya.com"
                R.id.domain_au1 -> "au1.gigya.com"
                R.id.domain_il1 -> "il1.gigya.com"
                R.id.domain_il5 -> "il5.gigya.com"
                else -> ""
            }

            val envId: Int = env_selection_group.checkedRadioButtonId
            val envString: String = when (envId) {
                R.id.env_prod -> ""
                R.id.env_st1 -> "-st1"
                R.id.env_st2 -> "-st2"
                else -> ""
            }

            val apiKeyString: String = api_key_sheet_edit.text.toString().trim()
            if (apiKeyString.isEmpty()) {
                api_key_sheet_edit.snackbar("Must enter new ApiService-Key for re-initialization")
            }

            var newDomainString: String = apiDomainString
            if (!envString.isEmpty()) {
                newDomainString = apiDomainString.substring(0, 3) + envString + apiDomainString.substring(3, apiDomainString.length)
            }

            Gigya.getInstance().init(apiKeyString, newDomainString)
            api_key_sheet_edit.snackbar("Re-initialized SDK")
            resultCallback.onReInit()
            dismiss()
        }
    }

    /**
     * Setup input dialog for sending an anonymous request.
     */
    private fun setupForAnonymous() {
        anonymous_sheet_send_button.setOnClickListener {
            val api = anonymous_sheet_edit.text.toString().trim()
            resultCallback.onAnonymousInput(api)
            dismiss()
        }
    }

    /**
     * Setup input dialog for social provider input.
     */
    private fun setupForLoginWithProvider() {
        login_with_provider_sheet_title.text = "Type supported provider name"
        login_with_provider_sheet_send_button.setOnClickListener {
            val provider = login_with_provider_sheet_edit.text.toString().trim()
            resultCallback.onLoginWithProvider(provider)
            dismiss()
        }
    }

    /**
     * Setup input dialog for sending login/registration requests.
     */
    private fun setupForLoginRegister() {
        login_register_sheet_title.text = when (type) {
            MainInputType.LOGIN -> "Login via username/password"
            MainInputType.REGISTER -> "Register via username/password"
            else -> {
                ""
            }
        }

        if (type == MainInputType.REGISTER) {
            login_register_session_exp_title.visible()
            login_register_session_exp_input.visible()
        }

        login_register_sheet_send_button.setOnClickListener {
            val username = login_register_sheet_username_edit.text.toString().trim()
            val password = login_register_sheet_password_edit.text.toString().trim()
            if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
                if (type == MainInputType.LOGIN) {
                    resultCallback.onLoginWith(username, password)
                } else if (type == MainInputType.REGISTER) {
                    val sessionExp = login_register_session_exp_input_edit.text.toString().trim().toInt()
                    resultCallback.onRegisterWith(username, password, sessionExp)
                }
                dismiss()
            }
        }
    }

    /**
     * Setup input dialog for sending a setAccount request.
     * Update options are hard coded for a specific example only.
     * Implement according to application requirements.
     */
    private fun setupForSetAccountInfo(pendingRegistration: Boolean) {
        set_account_sheet_title.text = "Set account info with selected parameters"
        set_account_sheet_send_button.setOnClickListener {
            val field = set_account_sheet_field_edit.text.toString().trim()
            val value = set_account_sheet_value_edit.text.toString().trim()
            resultCallback.onUpdateAccountWith(field, value, pendingRegistration)
            dismiss()
        }
    }

    private fun setupForAddOrRemoveConnection(type: MainInputType) {
        login_with_provider_sheet_title.text = "Select connection to social provider"
        login_with_provider_sheet_send_button.setOnClickListener {
            val provider = login_with_provider_sheet_edit.text.toString().trim()
            when (type) {
                MainInputType.REMOVE_CONNECTION -> resultCallback.onRemoveConnection(provider)
                MainInputType.ADD_CONNECTION -> resultCallback.onAddConnection(provider)
                else -> dismiss()
            }
            dismiss()
        }
    }
}