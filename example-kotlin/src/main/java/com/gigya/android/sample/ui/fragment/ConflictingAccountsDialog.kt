package com.gigya.android.sample.ui.fragment

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.gigya.android.sample.R
import com.gigya.android.sample.extras.visible
import com.gigya.android.sample.ui.MainViewModel
import kotlinx.android.synthetic.main.dialog_conflicting_accounts.*

class ConflictingAccountsDialog : androidx.fragment.app.DialogFragment() {

    private var viewModel: MainViewModel? = null

    lateinit var loginID: String

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
        // Populate LoginID.
        loginID = arguments!!["loginID"] as String
        ca_login_id.text = "Login id: " + loginID
        // Populate available providers
        val providers = arguments!!.getStringArrayList("providers")
        val providerAdapter = ArrayAdapter(context!!, android.R.layout.simple_spinner_dropdown_item, providers!!)
        ca_available_providers_spinner.adapter = providerAdapter
        ca_available_providers_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Stub.
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = providers[position]
                when (selected) {
                    "site" -> {
                        ca_password_edit_layout.visible()
                    }
                }
            }
        }

        ca_submit.setOnClickListener {
            val provider = ca_available_providers_spinner.selectedItem.toString()
            when (provider) {
                "site" -> {
                    val pass = ca_password_edit.text.toString().trim()
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