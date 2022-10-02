package com.gigya.android.sample.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.gigya.android.sample.R
import com.gigya.android.sample.databinding.FragmentLoginBinding
import com.gigya.android.sample.ui.MainActivity

class LoginFragment : BaseExampleFragment() {

    companion object {
        fun newInstance() = LoginFragment()
    }

    private var _binding: FragmentLoginBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as MainActivity).supportActionBar?.title = getString(R.string.title_login_fragment)
        setClicks()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setClicks() {
        binding.includeCredentialContent.login.setOnClickListener { credentialsLogin() }
        binding.includeCredentialContent.register.setOnClickListener { credentialsRegistration() }
        binding.includePasswordlessContent.passwordlessLogin.setOnClickListener { passwordlessLogin() }
        binding.includeSocialContent.socialLogin.setOnClickListener { socialLogin() }
        binding.includeScreensetsContent.useScreensets.setOnClickListener { useScreenSets() }
    }

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
                }, onLogin = {
            toastIt("login successful")
            activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.container, MyAccountFragment.newInstance())?.commitNow()
        })
    }

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
                error = {
                    // Display error.
                    toastIt("Error: ${it?.localizedMessage}")
                }, onLogin = {
            toastIt("login successful")
            activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.container, MyAccountFragment.newInstance())?.commitNow()
        })
    }

    private fun passwordlessLogin() {
        viewModel.passwordlessLogin(
                (activity as MainActivity).resultHandler,
                error = {
                    // Display error.
                    toastIt("Error: ${it?.localizedMessage}")
                }, onLogin = {
            toastIt("login successful")
            activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.container, MyAccountFragment.newInstance())?.commitNow()
        })
    }

    private fun socialLogin() {
        val provider = binding.includeSocialContent.socialProviderInput.text.toString().trim()
        if (provider.isEmpty()) {
            toastIt("Enter social provider")
            return
        }
        viewModel.socialLogin(provider,
                error = {
                    // Display error.
                    toastIt("Error: ${it?.localizedMessage}")
                },
                onLogin = {
                    toastIt("login successful")
                    activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.container, MyAccountFragment.newInstance())?.commitNow()
                })

    }

    private fun useScreenSets() {
        viewModel.showScreenSets(
                "Default-RegistrationLogin",
                error = {
                    // Display error.
                    toastIt("Error: ${it?.localizedMessage}")
                },
                onLogin = {
                    toastIt("login successful")
                    activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.container, MyAccountFragment.newInstance())?.commitNow()
                })
    }

    private fun useNativeScreenSets() {

    }


}
