package com.gigya.android.sample.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.gigya.android.sample.R
import com.gigya.android.sample.extras.invisible
import com.gigya.android.sample.extras.visible
import com.gigya.android.sample.ui.MainViewModel
import kotlinx.android.synthetic.main.fragment_otp_login.*

interface IOTPResultCallback {

    fun onOTPResult(json: String)
}


class CustomOTPRegistrationFragment : DialogFragment() {

    private var viewModel: MainViewModel? = null

    private var verifyState: Boolean = false

    lateinit var resultCallback: IOTPResultCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = activity?.run {
            ViewModelProvider(this).get(MainViewModel::class.java)
        } ?: throw Exception("Invalid Activity")
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
        g_otp_get_code_button.setOnClickListener {
            if (verifyState) {
                val code: String = g_otp_edit_text.text.toString().trim()
                verifyPhoneCode(code)
                return@setOnClickListener
            }
            val phoneNumber: String = g_otp_edit_text.text.toString().trim()
            loginByPhone(phoneNumber)
        }
    }

    private fun loginByPhone(phoneNumber: String) {
        g_top_progress.visible()
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
        g_top_progress.visible()
    }

    private fun switchToCodeVerificationView() {
        g_top_progress.invisible()
        verifyState = true
        g_otp_edit_text.setText("")
        g_otp_get_code_button.text = "Verify code"
        g_top_phone_hint.text = "Wait for verification code"
    }
}