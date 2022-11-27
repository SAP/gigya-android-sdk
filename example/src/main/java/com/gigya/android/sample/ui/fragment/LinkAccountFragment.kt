package com.gigya.android.sample.ui.fragment

import android.R
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.gigya.android.sample.databinding.FragmentLinkAccountBinding
import com.gigya.android.sample.repository.LinkInterruption

class LinkAccountFragment : BaseExampleFragment(), AdapterView.OnItemSelectedListener {

    companion object {
        fun newInstance() = LinkAccountFragment()
        const val name = "LinkAccountFragment"
        const val TAG = name
    }

    private var _binding: FragmentLinkAccountBinding? = null

    private val binding get() = _binding!!

    lateinit var interruption: LinkInterruption

    var selectedProvider: String? = null

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLinkAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindProviderSpinner()
        setClicks()
    }

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        selectedProvider = interruption.accounts.loginProviders[p2]
        Log.d(TAG, "selected: $selectedProvider")
        when (selectedProvider) {
            "site" -> {
                binding.linkSiteLayout.visible()
            }
            else -> {
                binding.linkSiteLayout.gone()
            }
        }
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        selectedProvider = interruption.accounts.loginProviders[0]
        Log.d(TAG, "nothing selected: $selectedProvider")
    }

    private fun bindProviderSpinner() {
        interruption.accounts.loginProviders.let { list ->
            val adapter: ArrayAdapter<String> = ArrayAdapter<String>(
                    requireContext(),
                    R.layout.simple_spinner_dropdown_item,
                    list)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.linkProvidersSpinner.adapter = adapter
            binding.linkProvidersSpinner.onItemSelectedListener = this
        }
    }

    private fun setClicks() {
        binding.linkButton.setOnClickListener {
            selectedProvider?.let { provider ->
                when (provider) {
                    "site" -> {
                        val loginId = interruption.accounts.loginID
                        val password = binding.linkSiteEdit.text.toString().trim()
                        viewModel.linkAccountSite(loginId, password)
                    }
                    else -> {
                        viewModel.linkAccountSocial(provider)
                    }
                }
                // Dismiss the fragment.
                requireActivity().onBackPressed()
            }
        }
    }
}