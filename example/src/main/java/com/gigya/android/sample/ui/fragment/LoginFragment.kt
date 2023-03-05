package com.gigya.android.sample.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.gigya.android.sample.R
import com.gigya.android.sample.databinding.FragmentLoginBinding
import com.gigya.android.sample.ui.MainActivity
import com.gigya.android.sdk.biometric.GigyaBiometric
import com.gigya.android.sdk.biometric.IGigyaBiometricCallback

class LoginFragment : BaseExampleFragment() {

    companion object {
        fun newInstance() = LoginFragment()
        const val name = "LoginFragment"
    }

    private var _binding: FragmentLoginBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as MainActivity).supportActionBar?.title = getString(R.string.title_login_fragment)
        (activity as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)
        (activity as MainActivity).supportActionBar?.setDisplayShowHomeEnabled(false)
        setClicks()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        if (biometric.isLocked) {
            evaluateBiometricSession(biometricCallback)
        }
    }

    private val biometricCallback = object : IGigyaBiometricCallback {
        override fun onBiometricOperationSuccess(action: GigyaBiometric.Action) {
            updateBiometricUiState()
            when (action) {
                GigyaBiometric.Action.OPT_IN -> {
                    toastIt("Biometric: OptIn")
                }
                GigyaBiometric.Action.OPT_OUT -> {
                    toastIt("Biometric: OptOut")
                }
                GigyaBiometric.Action.LOCK -> {
                    toastIt("Biometric: Locked")
                }
                GigyaBiometric.Action.UNLOCK -> {
                    toastIt("Biometric: Unlocked")
                    activity?.supportFragmentManager?.beginTransaction()
                            ?.replace(R.id.container, MyAccountFragment.newInstance())?.commit()
                }
            }
        }

        override fun onBiometricOperationFailed(reason: String?) {
            toastIt("Biometric authentication error: $reason")
            reason?.let { error ->
                if (error == "Key invalidated") {
                    toastIt("Key invalidated - session not recoverable")
                }
            }
        }

        override fun onBiometricOperationCanceled() {
            toastIt("Biometric operation canceled")
        }

    }

    private fun setClicks() {
        binding.includeCredentialContent.login.setOnClickListener { credentialsLogin() }
        binding.includeCredentialContent.register.setOnClickListener { credentialsRegistration() }
        binding.includePasswordlessContent.passwordlessLogin.setOnClickListener { passwordlessLogin() }
        binding.includePasswordlessContent.toOtpLogin.setOnClickListener { otpLogin() }
        binding.includeSocialContent.socialLogin.setOnClickListener { socialLogin() }
        binding.includeScreensetsContent.useScreensets.setOnClickListener { useScreenSets() }
        binding.includeScreensetsContent.useNativeScreensets.setOnClickListener { useNativeScreenSets() }
        binding.includeMobileSso.sso.setOnClickListener { sso() }
    }

    /**
     * Login using login/id credential pair.
     */
    private fun credentialsLogin() {
        val email = binding.includeCredentialContent.emailInput.text.toString().trim()
        val password = binding.includeCredentialContent.passwordInput.text.toString().trim()
        if (email.isEmpty() || password.isEmpty()) {
            toastIt("Credential field missing")
            return
        }
        hideKeyboard()
        viewModel.credentialLogin(
                email = email,
                password = password,
                error = {
                    // Display error.
                    toastIt("Error: ${it?.localizedMessage}")
                },
                onLogin = {
                    toastIt("login successful")
                    activity?.supportFragmentManager?.beginTransaction()
                            ?.replace(R.id.container, MyAccountFragment.newInstance())?.commit()
                },
                tfaInterruption = { interruption ->
                    val fragment = TFAFragment.newInstance()
                    fragment.interruption = interruption
                    activity?.supportFragmentManager?.beginTransaction()
                            ?.replace(R.id.container, fragment)
                            ?.addToBackStack(TFAFragment.name)
                            ?.commit()
                },
                linkInterruption = { interruption ->
                    val fragment = LinkAccountFragment.newInstance()
                    fragment.interruption = interruption
                    activity?.supportFragmentManager?.beginTransaction()
                            ?.replace(R.id.container, LinkAccountFragment.newInstance())
                            ?.addToBackStack(LinkAccountFragment.name)
                            ?.commit()
                },
        )
    }

    /**
     * Register using login/id credential pair.
     */
    private fun credentialsRegistration() {
        val email = binding.includeCredentialContent.emailInput.text.toString().trim()
        val password = binding.includeCredentialContent.passwordInput.text.toString().trim()
        if (email.isEmpty() || password.isEmpty()) {
            toastIt("Credential field missing")
            return
        }
        hideKeyboard()
        viewModel.credentialRegister(
                email = email,
                password = password,
                params = mutableMapOf(),
                error = {
                    // Display error.
                    toastIt("Error: ${it?.localizedMessage}")
                },
                onLogin = {
                    toastIt("login successful")
                    activity?.supportFragmentManager?.beginTransaction()
                            ?.replace(R.id.container, MyAccountFragment.newInstance())?.commitNow()
                },
                tfaInterruption = { interruption ->
                    val fragment = TFAFragment.newInstance()
                    fragment.interruption = interruption
                    activity?.supportFragmentManager?.beginTransaction()
                            ?.replace(R.id.container, fragment)
                            ?.addToBackStack(TFAFragment.name)
                            ?.commit()
                },
                linkInterruption = { interruption ->
                    val fragment = LinkAccountFragment.newInstance()
                    fragment.interruption = interruption
                    activity?.supportFragmentManager?.beginTransaction()
                            ?.replace(R.id.container, LinkAccountFragment.newInstance())
                            ?.addToBackStack(LinkAccountFragment.name)
                            ?.commit()
                },
        )
    }

    /**
     * Login using Fido passkey.
     */
    private fun passwordlessLogin() {
        viewModel.passwordlessLogin(
                (activity as MainActivity).resultHandler,
                error = {
                    // Display error.
                    toastIt("Error: ${it?.localizedMessage}")
                }, onLogin = {
            toastIt("login successful")
            activity?.supportFragmentManager?.beginTransaction()
                    ?.replace(R.id.container, MyAccountFragment.newInstance())?.commitNow()
        })
    }

    /**
     * Sign in via social provider.
     */
    private fun socialLogin() {
        val provider = binding.includeSocialContent.socialProviderInput.text.toString().trim()
        if (provider.isEmpty()) {
            toastIt("Enter social provider")
            return
        }
        viewModel.socialLogin(
                provider,
                error = {
                    // Display error.
                    toastIt("Error: ${it?.localizedMessage}")
                },
                onLogin = {
                    toastIt("login successful")
                    activity?.supportFragmentManager?.beginTransaction()
                            ?.replace(R.id.container, MyAccountFragment.newInstance())?.commitNow()
                },
                linkInterruption = { interruption ->
                    val fragment = LinkAccountFragment.newInstance()
                    fragment.interruption = interruption
                    activity?.supportFragmentManager?.beginTransaction()
                            ?.replace(R.id.container, fragment)
                            ?.addToBackStack(LinkAccountFragment.name)
                            ?.commit()
                },
        )

    }

    /**
     * Initiate web ScreenSets flow.
     */
    private fun useScreenSets() {
        viewModel.showScreenSets(
                "Default-RegistrationLogin",
                error = {
                    // Display error.
                    toastIt("Error: ${it?.localizedMessage}")
                },
                onLogin = {
                    toastIt("login successful")
                    activity?.supportFragmentManager?.beginTransaction()
                            ?.replace(R.id.container, MyAccountFragment.newInstance())?.commitNow()
                })
    }

    /**
     * Initiate Native ScreenSets flow.
     */
    private fun useNativeScreenSets() {
        viewModel.showNativeScreenSets(
                requireContext(),
                "example_tests",
                "login",
                error = {
                    // Display error.
                    toastIt("Error: ${it?.localizedMessage}")
                },
                onLogin = {
                    toastIt("login successful");
                    activity?.supportFragmentManager?.beginTransaction()
                            ?.replace(R.id.container, MyAccountFragment.newInstance())?.commitAllowingStateLoss()
                },
                onApiResult = { _, _ ->
                    // Stub.
                })
    }

    /**
     * Open OTP login Fragment.
     */
    private fun otpLogin() {
        activity?.supportFragmentManager?.beginTransaction()
                ?.replace(R.id.container, OTPLoginFragment.newInstance())
                ?.addToBackStack(OTPLoginFragment.name)
                ?.commit()
    }

    private fun sso() {
        viewModel.mobileSSO(
                error = {
                    // Display error.
                    toastIt("Error: ${it?.localizedMessage}")
                },
                onLogin = {
                    toastIt("login successful")
                    activity?.supportFragmentManager?.beginTransaction()
                            ?.replace(R.id.container, MyAccountFragment.newInstance())?.commitNow()
                },
        )
    }

}
