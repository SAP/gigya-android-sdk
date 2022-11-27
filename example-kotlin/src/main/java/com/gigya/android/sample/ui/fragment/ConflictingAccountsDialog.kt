package com.gigya.android.sample.ui.fragment

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.gigya.android.sample.R
import com.gigya.android.sample.extras.visible
import com.gigya.android.sample.ui.MainViewModel
import com.google.android.material.textfield.TextInputLayout

class ConflictingAccountsDialog : androidx.fragment.app.DialogFragment() {

    private var viewModel: MainViewModel? = null

    lateinit var loginID: String

    private var caTitle: TextView? = null
    private var caLoginId: TextView? = null
    private var caAvailableProvidersSpinner: Spinner? = null
    private var caPasswordEditLayout: TextInputLayout? = null
    private var caSubmit: Button? = null
    private var caPasswordEdit: EditText? = null

    companion object {

        fun newInstance(loginID: String, providers: ArrayList<String>): ConflictingAccountsDialog {
            val dialog = ConflictingAccountsDialog()
            val args = Bundle()
            args.putString("loginID", loginID)
            args.putStringArrayList("providers", providers)
            dialog.arguments = args
            dialog.isCancelable = false
            return dialog
        }
    }

    private fun findIds() {
        caTitle = requireView().findViewById(R.id.ca_title)
        caLoginId = requireView().findViewById(R.id.ca_login_id)
        caAvailableProvidersSpinner = requireView().findViewById(R.id.ca_available_providers_spinner)
        caPasswordEditLayout = requireView().findViewById(R.id.ca_password_edit_layout)
        caSubmit = requireView().findViewById(R.id.ca_submit)
        caPasswordEdit = requireView().findViewById(R.id.ca_password_edit)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = activity?.run {
            ViewModelProviders.of(this).get(MainViewModel::class.java)
        } ?: throw Exception("Invalid Activity")
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val layoutParams = dialog?.window?.attributes
        layoutParams?.width = ViewGroup.LayoutParams.MATCH_PARENT
        dialog?.window?.attributes = layoutParams
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_conflicting_accounts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupView()
    }

    private fun setupView() {
        findIds()
        // Populate LoginID.
        loginID = requireArguments()["loginID"] as String
        caLoginId!!.text = "Login id: " + loginID
        // Populate available providers
        val providers = requireArguments().getStringArrayList("providers")
        val providerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, providers!!)
        caAvailableProvidersSpinner!!.adapter = providerAdapter
        caAvailableProvidersSpinner!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Stub.
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = providers[position]
                when (selected) {
                    "site" -> {
                        caPasswordEditLayout!!.visible()
                    }
                }
            }
        }

        caSubmit!!.setOnClickListener {
            val provider = caAvailableProvidersSpinner!!.selectedItem.toString()
            when (provider) {
                "site" -> {
                    val pass = caPasswordEdit!!.text.toString().trim()
                    viewModel?.onLinkAccountWithSite(loginID, pass)
                }
                else -> {
                    viewModel?.onLinkAccountWithSocial(provider)
                }
            }
            dismissAllowingStateLoss()
        }
    }

    override fun dismissAllowingStateLoss() {
        super.dismissAllowingStateLoss()
    }

}