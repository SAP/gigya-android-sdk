package com.gigya.android.sample.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.gigya.android.sample.R
import com.gigya.android.sample.extras.invisible
import com.gigya.android.sample.extras.visible
import com.gigya.android.sample.ui.MainViewModel

interface IOTPResultCallback {

    fun onOTPResult(json: String)
}


class CustomOTPRegistrationFragment : DialogFragment() {

    private var viewModel: MainViewModel? = null

    private var verifyState: Boolean = false

    lateinit var resultCallback: IOTPResultCallback

    private var getCodeButton: Button? = null
    private var otpEditText: EditText? = null
    private var otpProgress: ProgressBar? = null
    private var otpPhoneHint: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = activity?.run {
            ViewModelProvider(this).get(MainViewModel::class.java)
        } ?: throw Exception("Invalid Activity")
    }

    private fun findIds() {
        getCodeButton = requireView().findViewById(R.id.g_otp_get_code_button)
        otpEditText = requireView().findViewById(R.id.g_otp_edit_text)
        otpProgress = requireView().findViewById(R.id.g_top_progress)
        otpPhoneHint = requireView().findViewById(R.id.g_top_phone_hint)
    }

    companion object {

        fun newInstance(resultCallback: IOTPResultCallback): CustomOTPRegistrationFragment {
            val fragment = CustomOTPRegistrationFragment()
            fragment.resultCallback = resultCallback
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog?.setCancelable(false);
        return inflater.inflate(R.layout.fragment_otp_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        findIds()
        getCodeButton!!.setOnClickListener {
            if (verifyState) {
                val code: String = otpEditText!!.text.toString().trim()
                verifyPhoneCode(code)
                return@setOnClickListener
            }
            val phoneNumber: String = otpEditText!!.text.toString().trim()
            loginByPhone(phoneNumber)
        }
    }

    private fun loginByPhone(phoneNumber: String) {
        otpProgress!!.visible()
        viewModel?.otpLoginByPhone(
                phoneNumber,
                onCodeSent = {
                    switchToCodeVerificationView()
                },
                success = { json ->
                    dismiss()
                    resultCallback.onOTPResult(json)
                },
                error = {
                    dismiss()
                })
    }

    private fun verifyPhoneCode(code: String) {
        viewModel?.onVerifyOTPCode(code)
        otpProgress!!.visible()
    }

    private fun switchToCodeVerificationView() {
        otpProgress!!.invisible()
        verifyState = true
        otpEditText!!.setText("")
        getCodeButton!!.text = "Verify code"
        otpPhoneHint!!.text = "Wait for verification code"
    }
}