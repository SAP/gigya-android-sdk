package com.gigya.android.sample.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.gigya.android.sample.R
import com.gigya.android.sample.databinding.FragmentMyAccountBinding
import com.gigya.android.sample.model.MyAccount
import com.gigya.android.sample.ui.MainActivity
import com.gigya.android.sdk.Gigya
import com.gigya.android.sdk.biometric.GigyaBiometric
import com.gigya.android.sdk.biometric.GigyaPromptInfo
import com.gigya.android.sdk.biometric.IGigyaBiometricCallback
import com.gigya.android.sdk.session.SessionStateObserver
import com.google.android.material.snackbar.Snackbar

class MyAccountFragment : BaseExampleFragment() {

    companion object {
        fun newInstance() = MyAccountFragment()
        const val name = "MyAccountFragment"
    }

    private var _binding: FragmentMyAccountBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onPause() {
        viewModel.account.removeObserver(nameObserver)
        Gigya.getInstance().unregisterSessionExpirationObserver(sessionExpirationObserver)
        super.onPause()
    }

    private val sessionExpirationObserver = SessionStateObserver {

        activity?.runOnUiThread {
            // Display error to user.
            Snackbar.make(
                requireView(),
                "Session expired!",
                Snackbar.LENGTH_LONG
            ).show()

            viewModel.logout(
                error = {
                    // Display error.
                    toastIt("Error: ${it?.localizedMessage}")
                },
                onLogout = {
                    toastIt("Account logout")
                    (activity as MainActivity).onLogout()
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        Gigya.getInstance().registerSessionExpirationObserver(sessionExpirationObserver)
        viewModel.account.observe(viewLifecycleOwner, nameObserver)
        // Check biometric state.
        if (biometric.isLocked) {
            evaluateBiometricSession(biometricCallback)
        }
        updateBiometricUiState()
    }

    private val nameObserver = Observer<MyAccount> { account ->
        binding.uidText.text = account.uid
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as MainActivity).supportActionBar?.title =
            getString(R.string.title_my_account_fragment)
        (activity as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        (activity as MainActivity).supportActionBar?.setDisplayShowHomeEnabled(true)

        setClicks()

        // Get account data if needed.
        populateAccountInfo()
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
                }
            }
        }

        override fun onBiometricOperationFailed(reason: String?) {
            toastIt("Biometric authentication error: $reason")
            reason?.let { error ->
                if (error == "Key invalidated" || error == "No fingerprints enrolled.") {
                    (activity as MainActivity).onLogout()
                }
            }
        }

        override fun onBiometricOperationCanceled() {
            toastIt("Biometric operation canceled")
        }

    }

    private fun populateAccountInfo() {
        if (viewModel.account.value == null) {
            viewModel.getAccount(
                error = {
                    // Display error.
                    toastIt("Error: ${it?.localizedMessage}")
                },
                success = {
                    toastIt("get account success")
                    // Good idea here to call get account info again
                    // to refresh the account data...
                },
            )
        }
    }

    private fun setClicks() {
        binding.logout.setOnClickListener {
            viewModel.logout(
                error = {
                    // Display error.
                    toastIt("Error: ${it?.localizedMessage}")
                },
                onLogout = {
                    toastIt("Account logout")
                    (activity as MainActivity).onLogout()
                }
            )
        }

        binding.getAccount.setOnClickListener {
            viewModel.getAccount(
                error = {
                    // Display error.
                    toastIt("Error: ${it?.localizedMessage}")
                },
                success = {
                    toastIt("get account success")
                    // Good idea here to call get account info again
                    // to refresh the account data...
                },
            )
        }

        binding.addConnection.setOnClickListener {
            val provider = binding.addRemoveEdit.text.toString().trim()
            if (provider.isEmpty()) {
                toastIt("Provider required")
                return@setOnClickListener
            }
            viewModel.addConnection(
                provider,
                error = {
                    // Display error.
                    toastIt("Error: ${it?.localizedMessage}")
                },
                success = {
                    toastIt("Connection added")
                    // Good idea here to call get account info again
                    // to refresh the account data...
                },
            )
        }

        binding.removeConnection.setOnClickListener {
            val provider = binding.addRemoveEdit.text.toString().trim()
            if (provider.isEmpty()) {
                toastIt("Provider required")
                return@setOnClickListener
            }
            viewModel.removeConnection(
                provider,
                error = {
                    // Display error.
                    toastIt("Error: ${it?.localizedMessage}")
                },
                success = {
                    toastIt("Connection removed")
                },
            )
        }

        binding.fidoRegister.setOnClickListener {
            viewModel.passwordlessRegister(
                (activity as MainActivity).resultHandler,
                error = {
                    // Display error.
                    toastIt("Error: ${it?.localizedMessage}")
                },
                success = {
                    toastIt("New Fido passkey added")
                }
            )
        }

        binding.fidoRevoke.setOnClickListener {
            viewModel.passwordlessRevoke(
                error = {
                    // Display error.
                    toastIt("Error: ${it?.localizedMessage}")
                },
                success = {
                    toastIt("Fido passkey revoked")
                }
            )
        }

        binding.biometricOpt.setOnClickListener {
            if (biometric.isOptIn) {
                biometric.optOut(
                    requireActivity(),
                    GigyaPromptInfo(
                        "Opt-Out requested",
                        "Place finger on sensor to continue", ""
                    ),
                    biometricCallback
                )
            } else {
                biometric.optIn(
                    requireActivity(),
                    GigyaPromptInfo(
                        "Opt-In requested",
                        "Place finger on sensor to continue", ""
                    ),
                    biometricCallback
                )
            }
        }

        binding.biometricLock.setOnClickListener {
            when (biometric.isLocked) {
                true -> {
                    biometric.unlock(
                        requireActivity(),
                        GigyaPromptInfo(
                            "Unlock session",
                            "Place finger on sensor to continue", ""
                        ),
                        biometricCallback
                    )
                }

                false -> {
                    biometric.lock(biometricCallback)
                }
            }
        }

        binding.accountScreensets.setOnClickListener {
            showScreenSets()
        }

        binding.accountNss.setOnClickListener {
            showNativeScreenSets()
        }

        binding.ssoExchangeInitiate.setOnClickListener {
            initiateSSOExchange()
        }

    }

    override fun updateBiometricUiState() {
        binding.biometricOpt.isEnabled = biometric.isAvailable
        binding.biometricLock.isEnabled = biometric.isAvailable && biometric.isOptIn
    }

    private fun showScreenSets() {
        viewModel.showScreenSets(
            binding.accountScreensetsNameEdit.text.toString().trim(),
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
    private fun showNativeScreenSets() {
        viewModel.showNativeScreenSets(
            requireContext(),
            binding.accountNativeScreensetsNameEdit.text.toString().trim(),
            "account-update",
            error = {
                // Display error.
                toastIt("Error: ${it?.localizedMessage}")
            },
            onLogin = {
                toastIt("login successful");
                activity?.supportFragmentManager?.beginTransaction()
                    ?.replace(R.id.container, MyAccountFragment.newInstance())
                    ?.commitAllowingStateLoss()
            },
            onApiResult = { api, gigyaApiResponse ->
                if (gigyaApiResponse != null) {
                    if (gigyaApiResponse.statusCode != 0) {
                        toastIt("Result success $api")
                    } else {
                        toastIt("Result error $api ${gigyaApiResponse.errorDetails}")
                    }
                }

            })
    }

    private fun initiateSSOExchange() {
        viewModel.getSSOExchangeToken(
            success = { code ->
                val url = "${
                    binding.ssoExchangeEditText.text.toString().trim()
                }?authCode=$code&gig_actions=sso.login"
                val fragment = SSOExchangeFragment.newInstance()
                val arguments = Bundle()
                arguments.putString("url", url)
                fragment.arguments = arguments
                activity?.supportFragmentManager?.beginTransaction()
                    ?.replace(R.id.container, fragment)?.commit()
            },
            error = {
                // Display error.
                toastIt("Error: ${it?.localizedMessage}")
            },
        )
    }

}