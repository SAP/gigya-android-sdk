package com.gigya.android.sample.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.gigya.android.sample.R
import com.gigya.android.sample.extras.gone
import com.gigya.android.sample.extras.loadBitmap
import com.gigya.android.sample.extras.visible
import com.gigya.android.sample.model.CountryCode
import com.gigya.android.sdk.interruption.GigyaResolver
import com.gigya.android.sdk.utils.UiUtils
import com.google.gson.Gson
import kotlinx.android.synthetic.main.dialog_tfa_registration.*
import kotlinx.android.synthetic.main.dialog_tfa_verification.*

class TFADialog : DialogFragment() {

    private var viewModel: MainViewModel? = null
    private lateinit var mode: String
    private var codes: Array<CountryCode>? = null

    companion object {

        fun newInstance(mode: String, providers: ArrayList<String>?): TFADialog {
            val dialog = TFADialog()
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
        loadCountryCodes()

        observeForQrCode()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val layoutParams = dialog?.window?.attributes
        layoutParams?.width = ViewGroup.LayoutParams.MATCH_PARENT
        layoutParams?.gravity = Gravity.TOP
        layoutParams?.height = ViewGroup.LayoutParams.WRAP_CONTENT
        dialog?.window?.attributes = layoutParams
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val layout = when (mode) {
            "registration" -> R.layout.dialog_tfa_registration
            "verification" -> R.layout.dialog_tfa_verification
            else -> 0
        }
        return inflater.inflate(layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        when (mode) {
            "registration" -> setupForRegistration()
            "verification" -> setupForVerification()
        }
    }

    private fun loadCountryCodes() {
        val json: String = context?.assets?.open("countryCodes.json")?.bufferedReader().use { it!!.readText() }
        codes = Gson().fromJson(json, Array<CountryCode>::class.java)
    }

    private fun setupForRegistration() {
        // Populate country code spinner.
        val codeAdapter = ArrayAdapter(context!!, android.R.layout.simple_spinner_dropdown_item, codes!!)
        country_spinner.adapter = codeAdapter

        val providers = arguments!!.getStringArrayList("providers")
        // Populate options spinner.
        val providerAdapter = ArrayAdapter(context!!, android.R.layout.simple_spinner_dropdown_item, providers!!)
        register_provider_spinner.adapter = providerAdapter
        register_provider_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Stub.
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (providers[position] == GigyaResolver.TFA_PHONE) {
                    true -> {
                        toggleViewsVisibility(method_group, phone_edit, phone_number, country_title, country_spinner, visibility = true)
                        toggleViewsVisibility(register_qr_image, register_step_1, register_step_2, qr_progress, register_generated_edit, visibility = false)
                        register_tfa_submit.text = "Get the code"
                    }
                    false -> {
                        toggleViewsVisibility(register_step_1, register_step_2, register_qr_image, qr_progress, register_generated_edit, visibility = true)
                        toggleViewsVisibility(method_group, phone_edit, phone_number, country_title, country_spinner, visibility = false)
                        register_tfa_submit.text = "Submit code"

                        // Register TOTP -> get QR code.
                        viewModel?.onTFATOTPRegister()
                    }
                }
            }
        }

        register_tfa_submit.setOnClickListener {
            val country = (country_spinner).selectedItem as CountryCode
            val method = register_provider_spinner.selectedItem.toString()
            when (method) {
                GigyaResolver.TFA_PHONE -> {
                    viewModel?.onTFAPhoneRegister(
                            country.dial_code + phone_edit.text.toString().trim().replace("+", ""),
                            when (method_group.checkedRadioButtonId) {
                                R.id.radio_sms -> "sms"
                                R.id.radio_voice -> "voice"
                                else -> "sms"
                            })
                    dismiss()
                }
                GigyaResolver.TFA_TOTP -> {
                    val code = register_generated_edit.text.toString().trim()
                    viewModel?.onTFATOTPCodeSubmit(code)
                    dismiss()
                }
            }

        }
    }

    private fun setupForVerification() {
        val providers = arguments!!.getStringArrayList("providers")
        // Populate options spinner.
        val providerAdapter = ArrayAdapter(context!!, android.R.layout.simple_spinner_dropdown_item, providers!!)
        verify_provider_spinner.adapter = providerAdapter

        verify_provider_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Stub.
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (providers[position] == GigyaResolver.TFA_PHONE) {
                    true -> {
                        toggleViewsVisibility(verify_tfa_send_code, visibility = true)
                    }
                    false -> {
                        toggleViewsVisibility(verify_tfa_send_code, visibility = false)
                    }
                }
            }
        }

        verify_tfa_send_code.setOnClickListener {
            val provider = verify_provider_spinner.selectedItem.toString()
            if (provider == GigyaResolver.TFA_PHONE) {
                viewModel?.onTFAPhoneVerify()
            }
        }

        verify_tfa_submit_code.setOnClickListener {
            val provider = verify_provider_spinner.selectedItem.toString()
            val code = verify_tfa_code_edit.text.toString().trim()
            when (provider) {
                GigyaResolver.TFA_PHONE -> viewModel?.onTFAPhoneCodeSubmit(code)
                GigyaResolver.TFA_TOTP -> viewModel?.onTFATOTPVerify(code)
            }
            dismiss()
        }
    }

    private fun observeForQrCode() {
        viewModel?.uiTrigger?.observe(this, Observer { dataPair ->
            @Suppress("UNCHECKED_CAST")
            when (dataPair?.first) {
                MainViewModel.UI_TRIGGER_SHOW_QR_CODE -> {
                    if (register_provider_spinner.selectedItem.toString() == GigyaResolver.TFA_TOTP) {
                        showQrCode(dataPair.second as String)
                    }
                }
            }
        })
    }

    private fun showQrCode(qrCode: String) {
        qr_progress.gone()
        register_qr_image.loadBitmap(UiUtils.bitmapFromBase64(qrCode))
    }

    private fun toggleViewsVisibility(vararg views: View, visibility: Boolean) {
        for (view in views) {
            when (visibility) {
                true -> view.visible()
                false -> view.gone()
            }
        }
    }
}