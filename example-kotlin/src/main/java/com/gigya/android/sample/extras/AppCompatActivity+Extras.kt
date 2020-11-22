package com.gigya.android.sample.extras

import android.app.Activity
import android.os.Handler
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.view.inputmethod.InputMethodManager

private const val ERROR_ALERT_DISMISSAL_DELAY: Long = 3000

fun AppCompatActivity.displayErrorAlert(title: Int, message: String) {
    displayErrorAlert(getString(title), message, false)
}

fun AppCompatActivity.displayErrorAlert(title: String, message: String) {
    displayErrorAlert(title, message, false)
}

fun AppCompatActivity.displayErrorAlert(title: String, message: String, delayedDismissal: Boolean) {
    val builder = AlertDialog.Builder(this)
    builder.setTitle(title).setMessage(message)
    builder.setPositiveButton(getString(android.R.string.ok)) { dialog, _ ->
        dialog.dismiss()
    }
    val dialog = builder.create()

    if (delayedDismissal) {
        Handler().postDelayed({
            dialog.dismiss()
        }, ERROR_ALERT_DISMISSAL_DELAY)
    }
    dialog.show()
}

fun AppCompatActivity.hideKeyboard() {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(findViewById<View>(android.R.id.content).windowToken, 0)
}
