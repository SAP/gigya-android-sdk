package com.gigya.android.sample.ui

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.gigya.android.sample.R
import com.gigya.android.sdk.Gigya
import com.gigya.android.sdk.api.RegisterApi
import com.gigya.android.sdk.network.GigyaError
import kotlinx.android.synthetic.main.sheet_anonymous.*
import kotlinx.android.synthetic.main.sheet_login_register.*
import kotlinx.android.synthetic.main.sheet_re_init.*
import kotlinx.android.synthetic.main.sheet_set_account.*
import org.jetbrains.anko.design.snackbar

class MainInputSheet : BottomSheetDialogFragment() {

    private var viewModel: MainViewModel? = null

    enum class MainInputType {
        ANONYMOUS, LOGIN, REGISTER, SET_ACCOUNT_INFO, REINIT
    }

    interface IApiResultCallback {
        fun onLoading()
        fun onJsonResult(json: String)
        fun onError(error: GigyaError)
        fun onReInit()
    }

    var type: MainInputType? = null

    lateinit var resultCallback: IApiResultCallback

    companion object {

        fun newInstance(type: MainInputType, resultCallback: IApiResultCallback): MainInputSheet {
            val args = Bundle()
            args.putSerializable("type", type)
            val fragment = MainInputSheet()
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val layout = when (type) {
            MainInputType.REINIT -> R.layout.sheet_re_init
            MainInputType.ANONYMOUS -> R.layout.sheet_anonymous
            MainInputType.LOGIN, MainInputType.REGISTER -> R.layout.sheet_login_register
            MainInputType.SET_ACCOUNT_INFO -> R.layout.sheet_set_account
            else -> 0
        }
        return inflater.inflate(layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        when (type) {
            MainInputType.REINIT -> setupForReInit()
            MainInputType.ANONYMOUS -> setupForAnonymous()
            MainInputType.REGISTER, MainInputType.LOGIN -> setupForLoginRegister()
            MainInputType.SET_ACCOUNT_INFO -> setupForSetAccountInfo()
        }
    }

    /**
     * Setup for SDK re-initialization option.
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
                api_key_sheet_edit.snackbar("Must enter new Api-Key for re-initialization")
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
     * Setup bottom sheet view for sending an anonymous request.
     */
    private fun setupForAnonymous() {
        anonymous_sheet_send_button.setOnClickListener {
            val api = anonymous_sheet_edit.text.toString().trim()
            if (!TextUtils.isEmpty(api)) {
                resultCallback.onLoading()
                viewModel?.sendAnonymous(api,
                        success = { json -> postSuccess(json) },
                        error = { possibleError -> postError(possibleError) })
            }
        }
    }

    /**
     * Setup bottom sheet view for sending login/registration requests.
     */
    private fun setupForLoginRegister() {
        login_register_sheet_title.text = when (type) {
            MainInputType.LOGIN -> "Login via username/password"
            MainInputType.REGISTER -> "Register via username/password"
            else -> {
                ""
            }
        }
        register_policy_radio_group.visibility = when (type) {
            MainInputType.LOGIN -> View.GONE
            MainInputType.REGISTER -> View.VISIBLE
            else -> {
                View.GONE
            }
        }

        login_register_sheet_send_button.setOnClickListener {
            val username = login_register_sheet_username_edit.text.toString().trim()
            val password = login_register_sheet_password_edit.text.toString().trim()
            if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
                resultCallback.onLoading()
                if (type == MainInputType.LOGIN) {
                    viewModel?.login(username, password,
                            success = { json -> postSuccess(json) },
                            error = { possibleError -> postError(possibleError) })
                } else if (type == MainInputType.REGISTER) {
                    viewModel?.register(username, password,
                            when (register_policy_radio_group.checkedRadioButtonId) {
                                R.id.policy_email -> RegisterApi.RegisterPolicy.EMAIL
                                R.id.policy_username -> RegisterApi.RegisterPolicy.USERNAME
                                else -> RegisterApi.RegisterPolicy.EMAIL_OR_USERNAME
                            },
                            success = { json -> postSuccess(json) },
                            error = { possibleError -> postError(possibleError) })
                }
            }
        }
    }

    /**
     * Setup bottom sheet view for sending a setAccount request.
     * Update options are hard coded for a specific example only.
     * Implement according to application requirements.
     */
    private fun setupForSetAccountInfo() {
        set_account_sheet_title.text = when (viewModel?.exampleSetup) {
            MainViewModel.SetupExample.BASIC -> {
                "Set account info (updating firstName)"
            }
            MainViewModel.SetupExample.CUSTOM_SCHEME -> {
                "Set account info custom (updating \"report\" custom data)"
            }
            else -> ""
        }

        set_account_sheet_send_button.setOnClickListener {
            val dummyData = set_account_sheet_edit.text.toString().trim()
            if (!TextUtils.isEmpty(dummyData)) {
                resultCallback.onLoading()
                viewModel?.setAccount(dummyData,
                        success = { json -> postSuccess(json) },
                        error = { possibleError -> postError(possibleError) })

            }
        }
    }

    private fun postSuccess(json: String) {
        resultCallback.onJsonResult(json)
        dismiss()
    }

    private fun postError(possibleError: GigyaError?) {
        possibleError?.let { error ->
            resultCallback.onError(error)
        }
        dismiss()
    }
}