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
import com.gigya.android.sdk.biometric.GigyaBiometric
import com.gigya.android.sdk.biometric.GigyaPromptInfo
import com.gigya.android.sdk.biometric.IGigyaBiometricCallback

class MyAccountFragment : BaseExampleFragment() {

    companion object {
        fun newInstance() = MyAccountFragment()
        const val name = "MyAccountFragment"
    }

    private var _binding: FragmentMyAccountBinding? = null

    private val binding get() = _binding!!

    private var biometric = GigyaBiometric.getInstance()

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
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        viewModel.account.observe(viewLifecycleOwner, nameObserver)
    }

    private val nameObserver = Observer<MyAccount> { account ->
        binding.uidText.text = account.uid
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as MainActivity).supportActionBar?.title = getString(R.string.title_my_account_fragment)
        (activity as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        (activity as MainActivity).supportActionBar?.setDisplayShowHomeEnabled(true)

        setClicks()

        // Get account data if needed.
        populateAccountInfo()

        // Check biometric state.
        if (biometric.isAvailable) {
            evaluateBiometricSession()
        }
        updateBiometricUiState()
    }

    private fun populateAccountInfo() {
        if (viewModel.account.value == null) {
            viewModel.getAccount { error ->
                error?.let {
                    // Display error.
                    toastIt("Error: ${it.localizedMessage}")
                }
            }
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
                        GigyaPromptInfo("Opt-Out requested",
                                "Place finger on sensor to continue", ""),
                        biometricCallback)
            } else {
                biometric.optIn(
                        requireActivity(),
                        GigyaPromptInfo("Opt-In requested",
                                "Place finger on sensor to continue", ""),
                        biometricCallback)
            }
        }

        binding.biometricLock.setOnClickListener {
            when (biometric.isLocked) {
                true -> {
                    biometric.unlock(
                            requireActivity(),
                            GigyaPromptInfo("Unlock session",
                                    "Place finger on sensor to continue", ""),
                            biometricCallback)
                }
                false -> {
                    biometric.lock(biometricCallback)
                }
            }
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
                }
            }
        }

        override fun onBiometricOperationFailed(reason: String?) {
            toastIt("Biometric authentication error: $reason")
        }

        override fun onBiometricOperationCanceled() {
            toastIt("Biometric operation canceled")
        }
    }

    private fun evaluateBiometricSession() {
        if (biometric.isLocked) {
            // Unlock the session
            biometric.unlock(
                    requireActivity(),
                    GigyaPromptInfo("Unlock session",
                            "Place finger on sensor to continue", ""),
                    biometricCallback
            )
        }
    }

    private fun updateBiometricUiState() {
        binding.biometricOpt.isEnabled = biometric.isAvailable
        binding.biometricLock.isEnabled = biometric.isAvailable && biometric.isOptIn
    }

}