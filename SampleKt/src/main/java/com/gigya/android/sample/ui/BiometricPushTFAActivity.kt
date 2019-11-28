package com.gigya.android.sample.ui

import android.os.Bundle
import com.gigya.android.sdk.GigyaDefinitions
import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.biometric.GigyaBiometric
import com.gigya.android.sdk.biometric.GigyaPromptInfo
import com.gigya.android.sdk.biometric.IGigyaBiometricCallback
import com.gigya.android.sdk.biometric.IGigyaBiometricOperationCallback
import com.gigya.android.sdk.tfa.R
import com.gigya.android.sdk.tfa.ui.PushTFAActivity

class BiometricPushTFAActivity : PushTFAActivity() {

    private var shouldLockSessionOnApproval: Boolean = false

    private val biometric: GigyaBiometric = GigyaBiometric.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        evaluateSessionState()
    }

    override fun alertOnCreate(): Boolean = false

    /**
     * Evaluate the current session encryption state.
     */
    private fun evaluateSessionState() {
        when (_tfaLib.sessionEncryption == GigyaDefinitions.SessionEncryption.FINGERPRINT) {
            true -> {
                if (!biometric.isAvailable) {
                    GigyaLogger.error("BiometricPushTFAActivity",
                            "Session is FINGERPRINT locked but biometric support is not available")
                    finish()
                    return
                }
                if (!biometric.isLocked) {
                    showActionAlert()
                    return
                }
                biometric.unlock(this,
                        GigyaPromptInfo(
                                getString(R.string.gig_tfa_biometric_locked_session_title),
                                getString(R.string.gig_tfa_biometric_locked_session_subtitle),
                                getString(R.string.gig_tfa_biometric_locked_session_description)
                        ),
                        object : IGigyaBiometricCallback {
                            override fun onBiometricOperationSuccess(action: GigyaBiometric.Action) {
                                GigyaLogger.debug("BiometricPushTFAActivity", "onBiometricOperationSuccess: Okay to approve push action")
                                shouldLockSessionOnApproval = true
                                showActionAlert()
                            }

                            override fun onBiometricOperationFailed(reason: String?) {
                                GigyaLogger.debug("BiometricPushTFAActivity", "onBiometricOperationFailed: - $reason - Available for retry")

                            }

                            override fun onBiometricOperationCanceled() {
                                GigyaLogger.debug("BiometricPushTFAActivity", "onBiometricOperationFailed: Push action is lost. Will call onDeny")
                                onDeny()
                            }
                        }
                )
            }
            false -> {
                showActionAlert()
            }
        }
    }

    /**
     * Overriding the approval action.
     */
    override fun onApprove(extras: Bundle?) {
        super.onApprove(extras)
        if (shouldLockSessionOnApproval) {
            shouldLockSessionOnApproval = false
            if (biometric.isAvailable) {
                biometric.lock(object : IGigyaBiometricOperationCallback {
                    override fun onBiometricOperationSuccess(action: GigyaBiometric.Action) {
                        GigyaLogger.debug("BiometricPushTFAActivity", "onBiometricOperationSuccess: ")
                    }

                    override fun onBiometricOperationFailed(reason: String?) {
                        GigyaLogger.error("BiometricPushTFAActivity ", "onBiometricOperationFailed: Session will remain unlocked")
                    }
                    
                })
            }
        }
    }

}