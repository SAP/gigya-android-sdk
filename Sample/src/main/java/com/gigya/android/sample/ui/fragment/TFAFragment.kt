package com.gigya.android.sample.ui.fragment

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.gigya.android.sample.R
import com.gigya.android.sample.extras.gone
import com.gigya.android.sample.extras.hideKeyboard
import com.gigya.android.sample.extras.loadBitmap
import com.gigya.android.sample.extras.visible
import com.gigya.android.sample.model.CountryCode
import com.gigya.android.sample.ui.MainActivity
import com.gigya.android.sample.ui.MainViewModel
import com.gigya.android.sdk.GigyaDefinitions
import com.gigya.android.sdk.model.tfa.TFAEmail
import com.gigya.android.sdk.model.tfa.TFARegisteredPhone
import com.google.gson.Gson
import kotlinx.android.synthetic.main.fragment_tfa.*
import org.jetbrains.anko.toast

class TFAFragment : Fragment(), BackPressListener {

    private var viewModel: MainViewModel? = null
    private lateinit var mode: String
    private var codes: Array<CountryCode>? = null

    companion object {

        fun newInstance(mode: String, providers: ArrayList<String>?): TFAFragment {
            val dialog = TFAFragment()
            val args = Bundle()
            args.putString("mode", mode)
            args.putStringArrayList("providers", providers)
            dialog.arguments = args
            return dialog
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = activity?.run {
            ViewModelProviders.of(this).get(MainViewModel::class.java)
        } ?: throw Exception("Invalid Activity")
        mode = arguments!!["mode"] as String
    }

    override fun onBackPressed() {
        // If back button is pressed make sure to call onCancel to indicate that the operation was
        // explicitly cancelled.
        activity?.let { mainActivity ->
            (mainActivity as MainActivity).onCancel()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_tfa, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        loadCountryCodes()
        observeUiTriggers()
        setupLayout()
    }

    /*
     Load the country code list from JSON asset file.
     */
    private fun loadCountryCodes() {
        val json: String = context?.assets?.open("countryCodes.json")?.bufferedReader().use { it!!.readText() }
        codes = Gson().fromJson(json, Array<CountryCode>::class.java)
    }

    /*
     Setup the main fragment layout.
     */
    private fun setupLayout() {
        // Setup TFA providers adapter
        val tfaProviders = arguments!!.getStringArrayList("providers")
        val tfaProviderAdapter = ArrayAdapter(context!!, android.R.layout.simple_spinner_dropdown_item, tfaProviders!!)
        tfa_providers_spinner.adapter = tfaProviderAdapter
        tfa_providers_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Stub.
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = tfaProviders[position]
                when (selected) {
                    GigyaDefinitions.TFA.EMAIL -> {
                        when (mode) {
                            "registration" -> {
                                // Not available for Email TFA setup.
                            }
                            "verification" -> {
                                tfa_get_code.text = "Verify Email"
                                toggleViewsVisibility(tfa_emails_spinner, visibility = true)
                                toggleViewsVisibility(tfa_qr_image_group, qr_code_image_progress, tfa_phone_registration_group, tfa_emails_spinner,
                                        tfa_verification_code_input_edit_layout, visibility = false)
                                // Start flow.
                                viewModel?.tfaVerificationResolver?.startVerifyWithEmail()
                            }
                        }
                    }
                    GigyaDefinitions.TFA.PHONE -> {
                        tfa_get_code.text = "Get code"
                        when (mode) {
                            "registration" -> {
                                toggleViewsVisibility(tfa_phone_registration_group, visibility = true)
                                toggleViewsVisibility(tfa_qr_image_group, tfa_verification_code_input_edit_layout, qr_code_image_progress,
                                        tfa_submit_code, tfa_get_code, visibility = false)
                            }
                            "verification" -> {
                                toggleViewsVisibility(tfa_verification_code_input_edit_layout, tfa_submit_code, visibility = true)
                                toggleViewsVisibility(tfa_phone_registration_group, tfa_qr_image_group, tfa_get_code, visibility = false)

                                // Verify Phone -> Request valid phone numbers.
                                viewModel?.tfaVerificationResolver?.startVerifyWithPhone()
                            }
                        }
                    }
                    GigyaDefinitions.TFA.TOTP -> {
                        tfa_get_code.text = "Get code"
                        when (mode) {
                            "registration" -> {
                                toggleViewsVisibility(tfa_qr_image_group, qr_code_image_progress,
                                        tfa_verification_code_input_edit_layout, tfa_submit_code, visibility = true)
                                toggleViewsVisibility(tfa_phone_registration_group, tfa_get_code,
                                        visibility = false)

                                // Register TOTP -> get QR code.
                                viewModel?.tfaRegistrationResolver?.startRegistrationWithTotp()
                            }
                            "verification" -> {
                                toggleViewsVisibility(tfa_qr_image_group, tfa_phone_registration_group, tfa_get_code, visibility = false)
                                toggleViewsVisibility(tfa_verification_code_input_edit_layout, tfa_submit_code, visibility = true)
                            }
                        }
                    }
                }
            }
        }

        // Setup country codes adapter
        val countryCodeAdapter = ArrayAdapter(context!!, android.R.layout.simple_spinner_dropdown_item, codes!!)
        country_code_spinner.adapter = countryCodeAdapter

        // Register phone number click listener.
        tfa_phone_submit.setOnClickListener {
            val countryCode = (country_code_spinner).selectedItem as CountryCode
            val phoneNumber = countryCode.dial_code + tfa_phone_number_input_edit.text.toString().trim().replace("+", "")
            val phoneVerificationMethod = when (phone_method_radio_group.checkedRadioButtonId) {
                R.id.radio_sms -> "sms"
                R.id.radio_voice -> "voice"
                else -> "sms"
            }
            // Update visibility.
            toggleViewsVisibility(tfa_phone_registration_group, visibility = false)
            toggleViewsVisibility(tfa_verification_code_input_edit_layout, tfa_submit_code, tfa_get_code, visibility = true)

            // Submit register.
            viewModel?.tfaRegistrationResolver?.startRegistrationWithPhone(phoneNumber, phoneVerificationMethod)
        }

        // Get code click listener
        tfa_get_code.setOnClickListener {
            val selectedTfaProvider = tfa_providers_spinner.selectedItem.toString()
            when (selectedTfaProvider) {
                GigyaDefinitions.TFA.PHONE -> {
                    when (mode) {
                        "verification" -> {
                            val wrapper = tfa_phone_numbers_spinner.selectedItem as TFAPoneWrapper
                            viewModel?.tfaVerificationResolver?.sendCodeToPhone(wrapper.phone!!)
                        }
                    }
                }
                GigyaDefinitions.TFA.EMAIL -> {
                    // Only supports verification mode.
                    val tfaEmail = (tfa_emails_spinner.selectedItem as TFAEmailWrapper).email
                    tfaEmail?.let {
                        viewModel?.tfaVerificationResolver?.sendCodeToeEmail(it)
                    }
                }
            }
        }

        // Submit verification code click listener
        tfa_submit_code.setOnClickListener {
            val code = tfa_verification_code_input_edit.text.toString().trim()
            val selectedTfaProvider = tfa_providers_spinner.selectedItem.toString()
            if (checkCode(code)) {
                when (selectedTfaProvider) {
                    GigyaDefinitions.TFA.PHONE -> {
                        when (mode) {
                            "registration" -> viewModel?.tfaRegistrationResolver?.verifyCode(GigyaDefinitions.TFA.PHONE, code)
                            "verification" -> viewModel?.tfaVerificationResolver?.verifyCode(GigyaDefinitions.TFA.PHONE, code)
                        }
                    }
                    GigyaDefinitions.TFA.TOTP -> {
                        when (mode) {
                            "registration" -> viewModel?.tfaRegistrationResolver?.verifyCode(GigyaDefinitions.TFA.TOTP, code)
                            "verification" -> viewModel?.tfaVerificationResolver?.verifyCode(GigyaDefinitions.TFA.TOTP, code)
                        }
                    }
                    GigyaDefinitions.TFA.EMAIL -> {
                        viewModel?.tfaVerificationResolver?.verifyCode(GigyaDefinitions.TFA.EMAIL, code)
                    }
                }
            }
            dismiss()
        }
    }

    /*
    Check code is available for submit. Prompt if not.
     */
    private fun checkCode(code: String): Boolean {
        if (code.isEmpty()) {
            activity?.toast("Code is empty... Please fill in provided authentication code")
            return false
        }
        return true
    }

    /*
    Explicitly dismiss fragment..
     */
    private fun dismiss() {
        activity?.let {
            (it as AppCompatActivity).hideKeyboard()
            it.onBackPressed()
        }
    }

    /*
    Observe uiTrigger LiveData variable from view model.
     */
    private fun observeUiTriggers() {
        viewModel?.uiTrigger?.observe(this, Observer { dataPair ->
            @Suppress("UNCHECKED_CAST")
            when (dataPair?.first) {
                MainViewModel.UI_TRIGGER_SHOW_TFA_QR_CODE -> {
                    if (tfa_providers_spinner.selectedItem.toString() == GigyaDefinitions.TFA.TOTP) {
                        showQrCode(dataPair.second as String)
                    }
                }
                MainViewModel.UI_TRIGGER_SHOW_TFA_PHONE_NUMBERS -> {
                    if (tfa_providers_spinner.selectedItem.toString() == GigyaDefinitions.TFA.PHONE) {
                        updateWithAvailablePhoneNumbersForVerification(dataPair.second as MutableList<TFARegisteredPhone>)
                    }
                }
                MainViewModel.UI_TRIGGER_SHOW_TFA_CODE_SENT -> activity?.toast("Verification code sent")
                MainViewModel.UI_TRIGGER_SHOW_TFA_EMAILS_AVAILABLE ->
                    updateWithAvailableEmailAddressesForVerification(dataPair.second as MutableList<TFAEmail>)
            }
        })
    }

    /*
    Update layout with available registered phone numbers.
     */
    private fun updateWithAvailablePhoneNumbersForVerification(phoneNumberList: MutableList<TFARegisteredPhone>) {
        val wrapped = phoneNumberList.map { TFAPoneWrapper(it) }
        val phoneNumberAdapter = ArrayAdapter(context!!, android.R.layout.simple_spinner_dropdown_item, wrapped)
        tfa_phone_numbers_spinner.adapter = phoneNumberAdapter
        toggleViewsVisibility(tfa_phone_numbers_spinner, tfa_get_code, visibility = true)
        toggleViewsVisibility(tfa_phone_registration_group, visibility = false)
    }

    /*
    Update layout with available registered email addresses.
     */
    private fun updateWithAvailableEmailAddressesForVerification(emailList: MutableList<TFAEmail>) {
        val wrapped = emailList.map { TFAEmailWrapper(it) }
        val emailAdapter = ArrayAdapter(context!!, android.R.layout.simple_spinner_dropdown_item, wrapped)
        tfa_emails_spinner.adapter = emailAdapter
        toggleViewsVisibility(tfa_emails_spinner, tfa_verification_code_input_edit_layout, visibility = true)
    }

    /*
    Decode and display provided QR code needed to register a TOTP authenticator application.
     */
    private fun showQrCode(qrCode: String) {
        qr_code_image_progress.gone()
        qr_code_image.loadBitmap(bitmapFromBase64(qrCode))
    }

    //region UTILITY

    private fun bitmapFromBase64(encodedImage: String): Bitmap {
        val decodedString = Base64.decode(encodedImage.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1], Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
    }

    private fun toggleViewsVisibility(vararg views: View, visibility: Boolean) {
        for (view in views) {
            when (visibility) {
                true -> view.visible()
                false -> view.gone()
            }
        }
    }

    //endregion

    //region WRAPPERS

    /**
     * Custom wrapper class for display purposes only.
     */
    inner class TFAPoneWrapper(phone: TFARegisteredPhone) {

        var phone: TFARegisteredPhone? = null

        init {
            this.phone = phone
        }

        override fun toString(): String {
            return phone?.obfuscated!!
        }
    }

    inner class TFAEmailWrapper(email: TFAEmail) {

        var email: TFAEmail? = null

        init {
            this.email = email
        }

        override fun toString(): String {
            return email?.obfuscated!!
        }
    }

    //endregion
}