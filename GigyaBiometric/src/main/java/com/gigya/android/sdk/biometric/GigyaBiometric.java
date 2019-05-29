package com.gigya.android.sdk.biometric;

import android.content.Context;

import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.biometric.v23.BiometricImplV23;
import com.gigya.android.sdk.biometric.v28.BiometricImplV28;
import com.gigya.android.sdk.containers.IoCContainer;

import javax.crypto.Cipher;

public class GigyaBiometric {
    //region static
    public enum Action {
        OPT_IN, OPT_OUT, LOCK, UNLOCK
    }

    public static final String VERSION = "android_1.0.0_beta_1";

    private static final String LOG_TAG = "GigyaBiometric";

    private static GigyaBiometric _sharedInstance;

    public static synchronized GigyaBiometric getInstance() {
        if (_sharedInstance == null) {
            IoCContainer container = Gigya.getContainer();

            container.bind(GigyaBiometric.class, GigyaBiometric.class, true);

            // Set the relevant biometric implementation according to Android API level.
            container.bind(BiometricImpl.class,
                    GigyaBiometricUtils.isPromptEnabled() ? BiometricImplV28.class : BiometricImplV23.class,
                    true);
            try {
                _sharedInstance = container.get(GigyaBiometric.class);
            } catch (Exception e) {
                GigyaLogger.error(LOG_TAG, "Error creating Gigya Biometric SDK (did you forget to Gigya.setApplication?");
                e.printStackTrace();
                throw new RuntimeException("Error creating Gigya Biometric SDK (did you forget to Gigya.setApplication?");
            }
        }
        return _sharedInstance;
    }
    // endregion

    private BiometricImpl _impl;
    private boolean _isAvailable;

    protected GigyaBiometric(Context context, BiometricImpl impl) {
        // Verify conditions for using biometric authentication.
        _isAvailable = verifyBiometricSupport(context);
        if (!_isAvailable) {
            return;
        }
        _impl = impl;
    }

    /**
     * Service availability evaluator.
     *
     * @return TRUE if biometric service is available for use.
     */
    public boolean isAvailable() {
        return _isAvailable;
    }

    /**
     * Optional animation state setter.
     * This is relevant only for devices running Android below Pie.
     *
     * @param animate Animation state.
     */
    public void setAnimationForPrePieDevices(boolean animate) {
        if (!GigyaBiometricUtils.isPromptEnabled()) {
            _impl.updateAnimationState(animate);
        }
    }

    /**
     * Verify that all needed conditions are available to use biometric operation.
     */
    private boolean verifyBiometricSupport(Context context) {
        if (!GigyaBiometricUtils.isSupported(context)) {
            GigyaLogger.error(LOG_TAG, "Fingerprint is not supported on this device. No sensor hardware was detected");
            return false;
        }
        if (!GigyaBiometricUtils.hasEnrolledFingerprints(context)) {
            GigyaLogger.error(LOG_TAG, "No fingerprint data available on device. Please enroll at least one fingerprint");
            return false;
        }
        return true;
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
     * Opts-in the existing session to use fingerprint authentication.
     *
     * @param gigyaPromptInfo   Prompt info containing title, subtitle & description for display.
     * @param biometricCallback Biometric authentication result callback.
     */
    public void optIn(final GigyaPromptInfo gigyaPromptInfo, final IGigyaBiometricCallback biometricCallback) {
        GigyaLogger.debug(LOG_TAG, "optIn: ");
        if (_impl.okayToOptInOut()) {
            _impl.showPrompt(Action.OPT_IN, gigyaPromptInfo, Cipher.ENCRYPT_MODE, biometricCallback);
        } else {
            final String failedMessage = "Session is invalid. Opt in operation is unavailable";
            GigyaLogger.error(LOG_TAG, failedMessage);
            biometricCallback.onBiometricOperationFailed(failedMessage);
        }
    }

    /**
     * Opts-out the existing session from using fingerprint authentication.
     *
     * @param gigyaPromptInfo   Prompt info containing title, subtitle & description for display.
     * @param biometricCallback Biometric authentication result callback.
     */
    public void optOut(final GigyaPromptInfo gigyaPromptInfo, final IGigyaBiometricCallback biometricCallback) {
        GigyaLogger.debug(LOG_TAG, "optOut: ");
        if (_impl.isLocked()) {
            GigyaLogger.error(LOG_TAG, "optOut: Need to unlock first before trying Opt-out operation");
            final String failedMessage = "Please unlock session before trying to Opt-out";
            biometricCallback.onBiometricOperationFailed(failedMessage);
            return;
        }
        if (_impl.okayToOptInOut()) {
            _impl.showPrompt(Action.OPT_OUT, gigyaPromptInfo, Cipher.DECRYPT_MODE, biometricCallback);
        } else {
            GigyaLogger.error(LOG_TAG, "optOut: Session is invalid. Opt in operation is unavailable");
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
    public void lock(final IGigyaBiometricCallback biometricCallback) {
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
     * @param gigyaPromptInfo   Prompt info containing title, subtitle & description for display.
     * @param biometricCallback Biometric authentication result callback.
     */
    public void unlock(final GigyaPromptInfo gigyaPromptInfo, final IGigyaBiometricCallback biometricCallback) {
        GigyaLogger.debug(LOG_TAG, "unlock: ");
        if (_impl.isOptIn()) {
            _impl.showPrompt(Action.UNLOCK, gigyaPromptInfo, Cipher.DECRYPT_MODE, biometricCallback);
        } else {
            final String failedMessage = "Not Opt-In";
            GigyaLogger.error(LOG_TAG, failedMessage);
            biometricCallback.onBiometricOperationFailed(failedMessage);
        }
    }

    //endregion
}
