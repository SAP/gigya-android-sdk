package com.gigya.android.sdk.biometric;

import android.content.Context;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.biometric.model.GigyaPromptInfo;
import com.gigya.android.sdk.biometric.utils.GigyaBiometricUtils;
import com.gigya.android.sdk.biometric.v23.GigyaBiometricImplV23;
import com.gigya.android.sdk.biometric.v28.GigyaBiometricImplV28;
import com.gigya.android.sdk.services.SessionService;

import javax.crypto.Cipher;

public class GigyaBiometric {

    private static final String LOG_TAG = "GigyaBiometric";

    private static final GigyaBiometric _sharedInstance = new GigyaBiometric();

    public static synchronized GigyaBiometric getInstance() {
        return _sharedInstance;
    }

    private GigyaBiometricImpl _impl;

    private boolean _isAvailable = true;

    public boolean isAvailable() {
        return _isAvailable;
    }

    private GigyaBiometric() {
        Gigya gigya = Gigya.getInstance();
        if (gigya == null) {
            GigyaLogger.error(LOG_TAG, "Gigya interface null. Please make sure" +
                    " to correctly instantiate the Gigya SDK within your main application class");
            _isAvailable = false;
            return;
        }
        // Check support conditions.
        final String verificationMessage = verifyBiometricSupport(gigya);
        if (verificationMessage != null) {
            _isAvailable = false;
            return;
        }
        // Reference session service.
        final SessionService sessionService = (SessionService) gigya.getGigyaComponent(SessionService.class);
        //_impl = new GigyaBiometricImplV23(sessionService);
        if (GigyaBiometricUtils.isPromptEnabled()) {
            _impl = new GigyaBiometricImplV28(sessionService);
        } else {
            _impl = new GigyaBiometricImplV23(sessionService);
        }
    }

    /**
     * Verify that all needed conditions are available to use biometric operation.
     */
    @Nullable
    private String verifyBiometricSupport(Gigya gigya) {
        String message = null;
        if (!GigyaBiometricUtils.isSupported(gigya.getContext())) {
            message = "Fingerprint is not supported on this device. No sensor hardware was detected";
            GigyaLogger.error(LOG_TAG, message);
        }
        if (!GigyaBiometricUtils.hasEnrolledFingerprints(gigya.getContext())) {
            message = "No fingerprint data available on device. Please enroll at least one fingerprint";
            GigyaLogger.error(LOG_TAG, message);
        }
        return message;
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
     * @param context           Available context.
     * @param gigyaPromptInfo   Prompt info containing title, subtitle & description for display.
     * @param biometricCallback Biometric authentication result callback.
     */
    public void optIn(Context context, final GigyaPromptInfo gigyaPromptInfo, final IGigyaBiometricCallback biometricCallback) {
        GigyaLogger.debug(LOG_TAG, "optIn: ");
        if (_impl.okayToOptInOut()) {
            _impl.showPrompt(context, Action.OPT_IN, gigyaPromptInfo, Cipher.ENCRYPT_MODE, biometricCallback, new Runnable() {
                @Override
                public void run() {
                    _impl.optIn(biometricCallback);
                }
            });
        } else {
            GigyaLogger.error(LOG_TAG, "Session is invalid. Opt in operation is unavailable");
        }
    }

    /**
     * Opts-out the existing session from using fingerprint authentication.
     *
     * @param context           Available context.
     * @param gigyaPromptInfo   Prompt info containing title, subtitle & description for display.
     * @param biometricCallback Biometric authentication result callback.
     */
    public void optOut(Context context, final GigyaPromptInfo gigyaPromptInfo, final IGigyaBiometricCallback biometricCallback) {
        GigyaLogger.debug(LOG_TAG, "optOut: ");
        if (_impl.okayToOptInOut()) {
            _impl.showPrompt(context, Action.OPT_OUT, gigyaPromptInfo, Cipher.DECRYPT_MODE, biometricCallback, new Runnable() {
                @Override
                public void run() {
                    _impl.optOut(biometricCallback);
                }
            });
        } else {
            GigyaLogger.error(LOG_TAG, "Session is invalid. Opt in operation is unavailable");
        }
    }

    /**
     * Locks the existing session until unlocking it.
     * No authenticated actions can be done while the session is locked.
     * Invokes the onError callback if the session is not opt-in.
     *
     * @param context           Available context.
     * @param gigyaPromptInfo   Prompt info containing title, subtitle & description for display.
     * @param biometricCallback Biometric authentication result callback.
     */
    public void lock(Context context, final GigyaPromptInfo gigyaPromptInfo, final IGigyaBiometricCallback biometricCallback) {
        GigyaLogger.debug(LOG_TAG, "lock: ");
        if (_impl.isOptIn()) {
            _impl.showPrompt(context, Action.LOCK, gigyaPromptInfo, Cipher.ENCRYPT_MODE, biometricCallback, new Runnable() {
                @Override
                public void run() {
                    // Lock the session.
                    _impl.lock(biometricCallback);
                }
            });
        } else {
            GigyaLogger.error(LOG_TAG, "Not Opt-in");
        }
    }


    /**
     * Unlocks the session so the user can continue to make authenticated actions.
     * Invokes the onError callback if the session is not opt-in.
     *
     * @param context           Available context.
     * @param gigyaPromptInfo   Prompt info containing title, subtitle & description for display.
     * @param biometricCallback Biometric authentication result callback.
     */
    public void unlock(Context context, final GigyaPromptInfo gigyaPromptInfo, final IGigyaBiometricCallback biometricCallback) {
        GigyaLogger.debug(LOG_TAG, "unlock: ");
        if (_impl.isOptIn()) {
            _impl.showPrompt(context, Action.UNLOCK, gigyaPromptInfo, Cipher.DECRYPT_MODE, biometricCallback, new Runnable() {
                @Override
                public void run() {
                    // Unlock the session.
                    _impl.unlock(biometricCallback);
                }
            });
        } else {
            GigyaLogger.error(LOG_TAG, "Not Opt-in");
        }
    }

    //endregion

    public enum Action {
        OPT_IN, OPT_OUT, LOCK, UNLOCK
    }

}
