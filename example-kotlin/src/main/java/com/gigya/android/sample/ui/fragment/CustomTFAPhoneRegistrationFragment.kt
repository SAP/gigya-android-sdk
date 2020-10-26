package com.gigya.android.sample.ui.fragment

import android.content.Context
import android.content.IntentFilter
import android.os.Bundle
import com.gigya.android.sample.SMSBroadcastReceiver
import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.tfa.ui.BaseTFAFragment
import com.gigya.android.sdk.tfa.ui.TFAPhoneRegistrationFragment
import com.google.android.gms.auth.api.phone.SmsRetriever


class CustomTFAPhoneRegistrationFragment : TFAPhoneRegistrationFragment() {

    lateinit var smsReceiver: SMSBroadcastReceiver

    companion object {

        fun newInstance(phoneProvider: String, language: String): CustomTFAPhoneRegistrationFragment {
            val fragment = CustomTFAPhoneRegistrationFragment()
            val args = Bundle()
            args.putString(ARG_PHONE_PROVIDER, phoneProvider)
            args.putString(BaseTFAFragment.ARG_LANGUAGE, language)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        val client = SmsRetriever.getClient(context)
        val task = client.startSmsRetriever()
        task.addOnSuccessListener {
            GigyaLogger.debug("TFA", "Successfully started retriever, expect broadcast intent")

        }
        task.addOnFailureListener {
            GigyaLogger.debug("TFA", "Failed to start retriever, inspect Exception for more details")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        smsReceiver = SMSBroadcastReceiver(codeListener)
    }

    override fun onStart() {
        super.onStart()
        activity?.registerReceiver(smsReceiver, IntentFilter("com.google.android.gms.auth.api.phone.SMS_RETRIEVED"))
    }

    override fun onStop() {
        activity?.unregisterReceiver(smsReceiver)
        super.onStop()
    }

    private val codeListener = object : SMSBroadcastReceiver.ISMSMessage {

        override fun onMessageCodeReceived(code: String) {
            _verificationCodeEditText.setText(code)
            _verificationCodeEditText.isEnabled = false
            _actionButton.callOnClick()
        }
    }


}