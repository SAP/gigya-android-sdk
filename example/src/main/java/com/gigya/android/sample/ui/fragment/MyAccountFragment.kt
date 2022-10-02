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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as MainActivity).supportActionBar?.title = getString(R.string.title_my_account_fragment)

        val nameObserver = Observer<MyAccount> { account ->
            binding.uidText.text = account.uid
        }
        viewModel.account.observe(viewLifecycleOwner, nameObserver)

        populateAccountInfo()
        setClicks()
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
    }


}