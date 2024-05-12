package com.gigya.android.sample.ui.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import com.gigya.android.sample.R
import com.gigya.android.sample.databinding.FragmentSsoExchangeBinding
import com.gigya.android.sample.ui.MainActivity

class SSOExchangeFragment : BaseExampleFragment() {

    companion object {
        fun newInstance() = SSOExchangeFragment()
        const val name = "SSOExchangeFragment"
    }

    private var _binding: FragmentSsoExchangeBinding? = null

    private var url: String? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSsoExchangeBinding.inflate(inflater, container, false)
        url = requireArguments().getString("url")
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as MainActivity).supportActionBar?.title =
            getString(R.string.title_sso_exchange_example)
        (activity as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        (activity as MainActivity).supportActionBar?.setDisplayShowHomeEnabled(true)

        if (url != null) {
            // javaScriptEnabled is mandatory for enabling JS SDK on WebView.
            val webSettings: WebSettings = binding.webview.settings
            webSettings.javaScriptEnabled = true
            binding.webview.loadUrl(url!!)
        }
    }


}