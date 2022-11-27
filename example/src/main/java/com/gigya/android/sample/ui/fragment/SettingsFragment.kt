package com.gigya.android.sample.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import com.gigya.android.sample.R
import com.gigya.android.sample.databinding.FragmentSettingsBinding
import com.gigya.android.sample.ui.MainActivity

class SettingsFragment : BaseExampleFragment() {

    companion object {
        fun newInstance() = SettingsFragment()
        const val name = "SettingsFragment"
    }

    private var _binding: FragmentSettingsBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.action_settings).isVisible = false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as MainActivity).supportActionBar?.title = getString(R.string.title_settings_fragment)
        (activity as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        (activity as MainActivity).supportActionBar?.setDisplayShowHomeEnabled(true)
        setClicks()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setClicks() {
        binding.changeApiKey.setOnClickListener { reInitWithNewApiKey() }
    }

    private fun reInitWithNewApiKey() {
        val apiKey = binding.editNewApiKey.text.toString().trim()
        val dataCenter = binding.editNewDataCenter.text.toString().trim()
        if (apiKey.isEmpty()) {
            toastIt("New ApiKey is required")
            return
        }
        viewModel.reinit(apiKey, dataCenter)
        toastIt("Done. Task is synchronous")
    }
}