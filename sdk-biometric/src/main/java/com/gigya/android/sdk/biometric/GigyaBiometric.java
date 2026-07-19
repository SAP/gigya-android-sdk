package com.gigya.android.sdk.biometric;

import android.content.Context;

import androidx.biometric.BiometricManager;
import androidx.fragment.app.FragmentActivity;

import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.biometric.v1.BiometricImplV1;
import com.gigya.android.sdk.containers.IoCContainer;
import com.gigya.android.sdk.reporting.ReportingManager;

import javax.crypto.Cipher;

public class GigyaBiometric {

    //region static

    public enum Action {
        OPT_IN, OPT_OUT, LOCK, UNLOCK
    }

    public static final String VERSION = "2.2.0";

    private static final String LOG_TAG = "GigyaBiometric";

    private static GigyaBiometric _sharedInstance;

    public static synchronized GigyaBiometric getInstance() {
        if (_sharedInstance == null) {
            IoCContainer container = Gigya.getContainer();

            container.bind(GigyaBiometric.class, GigyaBiometric.class, true);
            container.bind(BiometricImpl.class, BiometricImplV1.class, true);

            try {
                _sharedInstance = container.get(GigyaBiometric.class);
                GigyaLogger.debug(LOG_TAG, "Instantiation version: " + VERSION);
            } catch (Exception e) {
                GigyaLogger.error(LOG_TAG, "Error creating Gigya Biometric SDK (did you forget to Gigya.setApplication?");
                e.printStackTrace();
                throw new RuntimeException("Error instantiating Gigya Biometric SDK (did you forget to Gigya.setApplication?");
            }
        }
        return _sharedInstance;
    }
    // endregion

    private BiometricImpl _impl;

    protected GigyaBiometric(Context context, BiometricImpl impl) {
        _impl = impl;
    }

    /**
     * Service availability evaluator.
     *
     * @return TRUE if biometric service is available for use.
     */
    public boolean isAvailable() {
        return verifyBiometricSupport(_impl._context);
    }

    /**
     * @deprecated Animation is only relevant for the legacy pre-Pie fingerprint dialog which has
     * been removed. This method is now a no-op and will be removed in a future release.
     */
    @Deprecated
    public void setAnimationForPrePieDevices(boolean animate) {
        // no-op: custom fingerprint dialog removed in favour of Jetpack BiometricPrompt
    }

    /**
     * Verify that all needed conditions are available to use biometric operations.
     */
    private boolean verifyBiometricSupport(Context context) {
        int status = GigyaBiometricUtils.canAuthenticate(context);
        switch (status) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                return true;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                ReportingManager.get().error(VERSION, "biometric", "No biometric credentials enrolled on this device");
                GigyaLogger.error(LOG_TAG, "No biometric credentials enrolled on this device");
                return false;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
            default:
                ReportingManager.get().error(VERSION, "biometric", "Biometric hardware not available on this device");
                GigyaLogger.error(LOG_TAG, "Biometric hardware not available or temporarily unavailable");
                return false;
        }
    }

    //region BIOMETRIC OPERATIONS

    /**
     * Returns the indication if the session was opted-in.
     */
    public boolean isOptIn() {
        return (_impl != null && _impl.isOptIn());
    }

    /**
     * Returns the indication if the session is locked.
     */
    public boolean isLocked() {
        return (_impl != null && _impl.isLocked());
    }

    /**
     * Opts-in the existing session to use biometric authentication.
     *
     * @param activity          Initiator activity.
     * @param gigyaPromptInfo   Prompt info containing title, subtitle & description for display.
     * @param biometricCallback Biometric authentication result callback.
     */
    public void optIn(final FragmentActivity activity, final GigyaPromptInfo gigyaPromptInfo, final IGigyaBiometricCallback biometricCallback) {
        GigyaLogger.debug(LOG_TAG, "optIn: ");
        if (_impl.okayToOptInOut()) {
            _impl.showPrompt(activity, Action.OPT_IN, gigyaPromptInfo, Cipher.ENCRYPT_MODE, biometricCallback);
        } else {
            final String failedMessage = "Session is invalid. Opt in operation is unavailable";
            GigyaLogger.error(LOG_TAG, failedMessage);
            biometricCallback.onBiometricOperationFailed(failedMessage);
        }
    }

    /**
     * Opts-out the existing session from using biometric authentication.
     *
     * @param activity          Initiator activity.
     * @param gigyaPromptInfo   Prompt info containing title, subtitle & description for display.
     * @param biometricCallback Biometric authentication result callback.
     */
    public void optOut(final FragmentActivity activity, final GigyaPromptInfo gigyaPromptInfo, final IGigyaBiometricCallback biometricCallback) {
        GigyaLogger.debug(LOG_TAG, "optOut: ");
        if (_impl.isLocked()) {
            GigyaLogger.error(LOG_TAG, "optOut: Need to unlock first before trying Opt-out operation");
            final String failedMessage = "Please unlock session before trying to Opt-out";
            biometricCallback.onBiometricOperationFailed(failedMessage);
            return;
        }
        if (_impl.okayToOptInOut()) {
            _impl.showPrompt(activity, Action.OPT_OUT, gigyaPromptInfo, Cipher.DECRYPT_MODE, biometricCallback);
        } else {
            GigyaLogger.error(LOG_TAG, "optOut: Session is invalid. Opt out operation is unavailable");
            final String failedMessage = "Invalid session. Unable to perform biometric operation";
            biometricCallback.onBiometricOperationFailed(failedMessage);
        }
    }

    /**
     * Locks the existing session until unlocking it.
     * No authenticated actions can be done while the session is locked.
     * Invokes the onError callback if the session is not opt-in.
     *
     * @param biometricCallback Biometric authentication result callback.
     */
    public void lock(final IGigyaBiometricOperationCallback biometricCallback) {
        GigyaLogger.debug(LOG_TAG, "lock: ");
        if (_impl.isOptIn()) {
            _impl.lock(biometricCallback);
        } else {
            final String failedMessage = "Not Opt-In";
            GigyaLogger.error(LOG_TAG, failedMessage);
            biometricCallback.onBiometricOperationFailed(failedMessage);
        }
    }

    /**
     * Unlocks the session so the user can continue to make authenticated actions.
     * Invokes the onError callback if the session is not opt-in.
     *
     * @param activity          Initiator activity.
     * @param gigyaPromptInfo   Prompt info containing title, subtitle & description for display.
     * @param biometricCallback Biometric authentication result callback.
     */
    public void unlock(final FragmentActivity activity, final GigyaPromptInfo gigyaPromptInfo, final IGigyaBiometricCallback biometricCallback) {
        GigyaLogger.debug(LOG_TAG, "unlock: ");
        if (_impl.isOptIn()) {
            _impl.showPrompt(activity, Action.UNLOCK, gigyaPromptInfo, Cipher.DECRYPT_MODE, biometricCallback);
        } else {
            final String failedMessage = "Not Opt-In";
            GigyaLogger.error(LOG_TAG, failedMessage);
            biometricCallback.onBiometricOperationFailed(failedMessage);
        }
    }

    //endregion
}
