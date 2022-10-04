package com.gigya.android.sample.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.gigya.android.sample.R
import com.gigya.android.sample.databinding.FragmentOtpLoginBinding
import com.gigya.android.sample.ui.MainActivity

class OTPLoginFragment : BaseExampleFragment() {

    companion object {
        fun newInstance() = OTPLoginFragment()
        const val name = "OTPLoginFragment"
    }

    private var _binding: FragmentOtpLoginBinding? = null

    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOtpLoginBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as MainActivity).supportActionBar?.title = getString(R.string.title_otp_login_fragment)
        (activity as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        (activity as MainActivity).supportActionBar?.setDisplayShowHomeEnabled(true)
        setClicks()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setClicks() {
        binding.includeOtpContent.optLogin.setOnClickListener { otpLogin() }
        binding.includeOtpContent.otpVerify.setOnClickListener { otpVerify() }
    }

    /**
     * Login using phone number OTP
     */
    private fun otpLogin() {
        val phoneNumber = binding.includeOtpContent.phoneNumberInput.text.toString().trim()
        if (phoneNumber.isEmpty()) {
            toastIt("Phone number required")
            return
        }
        viewModel.otpLogin(
                phoneNumber,
                onLogin = {
                    toastIt("login successful")
                    activity?.supportFragmentManager?.beginTransaction()
                            ?.replace(R.id.container, MyAccountFragment.newInstance())?.commitNow()
                },
                error = {
                    // Display error.
                    toastIt("Error: ${it?.localizedMessage}")
                },
                onPendingOTP = {
                    // Display UI for code verification.
                    binding.includeOtpContent.otpVerifyLayout.visibility = View.VISIBLE
                },
        )
    }

    private fun otpVerify() {
        val code = binding.includeOtpContent.codeInput.text.toString().trim()
        if (code.isEmpty()) {
            toastIt("Code required")
            return
        }
        viewModel.otpVerify(code)
    }
}