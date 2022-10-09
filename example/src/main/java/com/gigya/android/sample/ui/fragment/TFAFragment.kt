package com.gigya.android.sample.ui.fragment

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.gigya.android.sample.R
import com.gigya.android.sample.databinding.FragmentTfaBinding
import com.gigya.android.sample.repository.TFAInterruption
import com.gigya.android.sample.repository.TFAInterruptionType
import com.gigya.android.sample.ui.MainActivity

/**
 * TFA fragment used for showcasing Gigya TFA SDK usage.
 * The fragment is bound to TOTP level 20 RBA for demonstrating the use of the relevant
 * resolver flows.
 */
class TFAFragment : BaseExampleFragment(), AdapterView.OnItemSelectedListener {

    companion object {
        fun newInstance() = TFAFragment()
        const val name = "TFAFragment"
    }

    private var _binding: FragmentTfaBinding? = null

    private val binding get() = _binding!!

    lateinit var interruption: TFAInterruption

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTfaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as MainActivity).supportActionBar?.title = getString(R.string.title_tfa_fragment)
        (activity as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)
        (activity as MainActivity).supportActionBar?.setDisplayShowHomeEnabled(false)

        bindUiData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        // Selected TFA type. This example currently supports TOTP only for demonstration purposes.
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        // Who cares.
    }

    private fun bindUiData() {
        interruption.providers.let { list ->
            val names = list.map { it.name }
            val adapter: ArrayAdapter<String> = ArrayAdapter<String>(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    names)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.providersSpinner.adapter = adapter
            binding.providersSpinner.onItemSelectedListener = this
        }
        selectInterruptionType()
    }

    /**
     * Control registration/verification UI interruption state.
     */
    private fun selectInterruptionType() {
        when (interruption.type) {
            TFAInterruptionType.REGISTRATION -> {
                bindRegistrationLayout()

            }
            TFAInterruptionType.VERIFICATION -> {
                bindVerificationLayout()
            }
        }
    }

    private fun bindRegistrationLayout() {
        viewModel.registerTfaTotp(
                onQrCode = { qrCode ->
                    // Decoding the image received (Base64).
                    val decoded: ByteArray = Base64.decode(qrCode.split(",").toTypedArray()[1], Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                    binding.tfaAuthImage.setImageBitmap(bitmap)
                },
                error = {
                    toastIt("Error: ${it?.localizedMessage}")
                }
        )

        binding.tfaVerifyCodeButton.setOnClickListener {
            val code = binding.tfaAuthCodeEdit.text.toString().trim()
            viewModel.verifyTotpCode(
                    code,
                    error = {
                        toastIt("Error: ${it?.localizedMessage}")
                    },
                    onLogin = {
                        // TOTP login success. Original callback will update.
                        requireActivity().onBackPressed()
                    },
            )
        }

    }

    private fun bindVerificationLayout() {

    }


}