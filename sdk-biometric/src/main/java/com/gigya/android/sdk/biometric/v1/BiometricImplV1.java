package com.gigya.android.sdk.biometric.v1;

import android.content.Context;
import android.os.Build;
import android.security.keystore.KeyPermanentlyInvalidatedException;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.biometric.BiometricImpl;
import com.gigya.android.sdk.biometric.GigyaBiometric;
import com.gigya.android.sdk.biometric.GigyaPromptInfo;
import com.gigya.android.sdk.biometric.IGigyaBiometricCallback;
import com.gigya.android.sdk.biometric.R;
import com.gigya.android.sdk.persistence.IPersistenceService;
import com.gigya.android.sdk.session.ISessionService;

import java.util.concurrent.Executor;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

public class BiometricImplV1 extends BiometricImpl {

    private static final String LOG_TAG = "BiometricImplV1";

    public BiometricImplV1(Context context, Config config, ISessionService sessionService, IPersistenceService persistenceService) {
        super(context, config, sessionService, persistenceService);
    }

    @Override
    protected void updateAnimationState(boolean state) {
        // no-op: animation was only relevant for the removed custom pre-Pie fingerprint dialog
    }

    @Override
    synchronized public void showPrompt(final FragmentActivity activity,
                                        final GigyaBiometric.Action action,
                                        @NonNull GigyaPromptInfo gigyaPromptInfo,
                                        int encryptionMode,
                                        final @NonNull IGigyaBiometricCallback callback) {
        final SecretKey key = _biometricKey.getKey();
        if (key == null) {
            GigyaLogger.error(LOG_TAG, "Unable to generate secret key from KeyStore API");
            callback.onBiometricOperationFailed("Unable to generate secret key");
            return;
        }
        if (activity == null) {
            GigyaLogger.error(LOG_TAG, "Null activity context provided");
            callback.onBiometricOperationFailed("Null activity context provided");
            return;
        }
        if (activity.isFinishing() || activity.isDestroyed()) {
            GigyaLogger.error(LOG_TAG, "Activity state is invalid");
            callback.onBiometricOperationFailed("Activity state is invalid");
            return;
        }

        final Cipher cipher;
        try {
            cipher = (encryptionMode == Cipher.DECRYPT_MODE)
                    ? _biometricKey.getDecryptionCipher(key)
                    : _biometricKey.getEncryptionCipher(key);
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && cause instanceof KeyPermanentlyInvalidatedException) {
                GigyaLogger.error(LOG_TAG, cause.getMessage());
                onInvalidKey();
                callback.onBiometricOperationFailed("Biometric key invalidated — please opt-in again");
                return;
            }
            GigyaLogger.error(LOG_TAG, "Failed to initialize cipher: " + e.getMessage());
            callback.onBiometricOperationFailed("Failed to initialize cipher");
            return;
        }

        if (cipher == null) {
            GigyaLogger.error(LOG_TAG, "Failed to initialize cipher");
            callback.onBiometricOperationFailed("Failed to initialize cipher");
            return;
        }

        final BiometricPrompt.CryptoObject cryptoObject = new BiometricPrompt.CryptoObject(cipher);

        final String title = gigyaPromptInfo.getTitle() != null
                ? gigyaPromptInfo.getTitle()
                : _context.getString(R.string.bio_prompt_default_title);
        final String subtitle = gigyaPromptInfo.getSubtitle() != null
                ? gigyaPromptInfo.getSubtitle()
                : _context.getString(R.string.bio_prompt_default_subtitle);
        final String description = gigyaPromptInfo.getDescription();

        BiometricPrompt.PromptInfo.Builder promptBuilder = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText(_context.getString(android.R.string.cancel))
                .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG);

        if (description != null && !description.isEmpty()) {
            promptBuilder.setDescription(description);
        }

        final BiometricPrompt.PromptInfo promptInfo = promptBuilder.build();

        Executor executor = ContextCompat.getMainExecutor(_context);

        BiometricPrompt biometricPrompt = new BiometricPrompt(activity, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        GigyaLogger.error(LOG_TAG, "onAuthenticationError: " + errString);
                        if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                                || errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                            callback.onBiometricOperationCanceled();
                        } else {
                            callback.onBiometricOperationFailed(errString.toString());
                        }
                    }

                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        GigyaLogger.debug(LOG_TAG, "onAuthenticationSucceeded");
                        onSuccessfulAuthentication(cipher, action, callback);
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        GigyaLogger.debug(LOG_TAG, "onAuthenticationFailed");
                        // Jetpack handles retry UI internally; no callback needed here
                    }
                });

        biometricPrompt.authenticate(promptInfo, cryptoObject);
    }
}
