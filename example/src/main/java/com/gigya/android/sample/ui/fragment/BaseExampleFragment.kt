package com.gigya.android.sample.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.gigya.android.sample.ui.MainActivity
import com.gigya.android.sample.ui.MainViewModel
import com.gigya.android.sdk.biometric.GigyaBiometric
import com.gigya.android.sdk.biometric.GigyaPromptInfo
import com.gigya.android.sdk.biometric.IGigyaBiometricCallback

open class BaseExampleFragment : Fragment() {

    open val viewModel: MainViewModel by activityViewModels()

    open var biometric = GigyaBiometric.getInstance()!!

    fun toastIt(message: String) {
        if (isAdded) {
            val toast = Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT)
            toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 0)
            toast.show()
        }
    }

    open fun updateBiometricUiState() {
        // Stub
    }

    open fun evaluateBiometricSession(callback: IGigyaBiometricCallback) {
        if (biometric.isLocked) {
            // Unlock the session
            biometric.unlock(
                    requireActivity(),
                    GigyaPromptInfo("Unlock session",
                            "Place finger on sensor to continue", ""),
                    callback
            )
        }
    }
}

fun Fragment.hideKeyboard() {
    val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(requireView().windowToken, 0)
}

fun Fragment.clearBackStack() {
    val fm: FragmentManager = requireActivity().supportFragmentManager
    for (i in 0 until fm.backStackEntryCount) {
        fm.popBackStack()
    }
}

fun View.visible() {
    this.visibility = View.VISIBLE
}

fun View.invisible() {
    this.visibility = View.INVISIBLE
}

fun View.gone() {
    this.visibility = View.GONE
}
