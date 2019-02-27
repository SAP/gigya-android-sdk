package com.gigya.android.sample.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
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
import kotlinx.android.synthetic.main.dialog_tfa.*

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
            dialog.isCancelable = false
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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val layoutParams = dialog?.window?.attributes
        layoutParams?.width = ViewGroup.LayoutParams.MATCH_PARENT
        layoutParams?.gravity = Gravity.TOP
        layoutParams?.height = ViewGroup.LayoutParams.WRAP_CONTENT
        dialog?.window?.attributes = layoutParams
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // dialog?.window?.setBackgroundDrawable(getRoundedCornerBackground())
        return inflater.inflate(R.layout.dialog_tfa, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        loadCountryCodes()
        observeUiTriggers()
        setupLayout()
    }

    private fun loadCountryCodes() {
        val json: String = context?.assets?.open("countryCodes.json")?.bufferedReader().use { it!!.readText() }
        codes = Gson().fromJson(json, Array<CountryCode>::class.java)
    }

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
                    GigyaResolver.TFA_PHONE -> {
                        when (mode) {
                            "registration" -> {
                                toggleViewsVisibility(tfa_phone_registration_group, visibility = true)
                                toggleViewsVisibility(tfa_qr_image_group, tfa_input_group, visibility = false)
                            }
                            "verification" -> {
                                // Verify Phone -> Get the code.
                                viewModel?.onTFAPhoneVerify()
                                toggleViewsVisibility(tfa_input_group, visibility = true)
                                toggleViewsVisibility(tfa_phone_registration_group, tfa_qr_image_group,
                                        visibility = false)
                            }
                        }
                    }
                    GigyaResolver.TFA_TOTP -> {
                        when (mode) {
                            "registration" -> {
                                // Register TOTP -> get QR code.
                                viewModel?.onTFATOTPRegister()
                                toggleViewsVisibility(tfa_qr_image_group, tfa_input_group, visibility = true)
                                toggleViewsVisibility(tfa_phone_registration_group,
                                        visibility = false)
                            }
                            "verification" -> {
                                toggleViewsVisibility(tfa_qr_image_group, tfa_phone_registration_group, visibility = false)
                                toggleViewsVisibility(tfa_input_group, visibility = true)
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
            viewModel?.onTFAPhoneRegister(phoneNumber, phoneVerificationMethod)
            toggleViewsVisibility(tfa_phone_registration_group, visibility = false)
            toggleViewsVisibility(tfa_input_group, visibility = true)
        }

        // Get code click listener
        tfa_get_code.setOnClickListener {
            viewModel?.onTFAPhoneVerify()
        }

        // Submit verification code click listener
        tfa_submit_code.setOnClickListener {
            val code = tfa_verification_code_input_edit.text.toString().trim()
            val selectedTfaProvider = tfa_providers_spinner.selectedItem.toString()
            when (selectedTfaProvider) {
                GigyaResolver.TFA_PHONE -> {
                    viewModel?.onTFAPhoneCodeSubmit(code)
                }
                GigyaResolver.TFA_TOTP -> {
                    when (mode) {
                        "registration" -> viewModel?.onTFATOTPCodeSubmit(code)
                        "verification" -> viewModel?.onTFATOTPVerify(code)
                    }
                }
            }
            dismissAllowingStateLoss()
        }

        // Dialog dismissal click listener.
        tfa_dismiss_dialog.setOnClickListener {
            dismissAllowingStateLoss()
        }
    }

    private fun observeUiTriggers() {
        viewModel?.uiTrigger?.observe(this, Observer { dataPair ->
            @Suppress("UNCHECKED_CAST")
            when (dataPair?.first) {
                MainViewModel.UI_TRIGGER_SHOW_QR_CODE -> {
                    if (tfa_providers_spinner.selectedItem.toString() == GigyaResolver.TFA_TOTP) {
                        showQrCode(dataPair.second as String)
                    }
                }
            }
        })
    }

    private fun showQrCode(qrCode: String) {
        qr_code_image_progress.gone()
        qr_code_image.loadBitmap(UiUtils.bitmapFromBase64(qrCode))
    }

    private fun toggleViewsVisibility(vararg views: View, visibility: Boolean) {
        for (view in views) {
            when (visibility) {
                true -> view.visible()
                false -> view.gone()
            }
        }
    }

    private fun getRoundedCornerBackground(): Drawable {
        val gradientDrawable = GradientDrawable()
        gradientDrawable.setColor(Color.WHITE)
        gradientDrawable.cornerRadius = 16f
        return gradientDrawable
    }
}