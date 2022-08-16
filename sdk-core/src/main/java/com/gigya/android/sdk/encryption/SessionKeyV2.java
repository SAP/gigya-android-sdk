package com.gigya.android.sdk.encryption;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import androidx.annotation.RequiresApi;

import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.reporting.ReportingManager;

import java.security.KeyStore;
import java.security.KeyStoreException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class SessionKeyV2 {

    private String getAlias() {
        return "GS_ALIAS_V2";
    }

    public static boolean isUsed() {
        final String ANDROID_KEY_STORE = "AndroidKeyStore";
        final KeyStore keyStore;
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            if (keyStore.containsAlias("GS_ALIAS_V2")) {
                // Remove old keystore alias for obsolete encryption key prior to v2.
                if (keyStore.containsAlias("GS_ALIAS")) {
                    keyStore.deleteEntry("GS_ALIAS");
                }
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public SecretKey getKey() throws EncryptionException {
        try {
            final String ANDROID_KEY_STORE = "AndroidKeyStore";
            final KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            if (keyStore.containsAlias(getAlias())) {
                // Alias available. Key generated.
                final KeyStore.SecretKeyEntry secretKeyEntry = (KeyStore.SecretKeyEntry) keyStore.getEntry(getAlias(), null);
                return secretKeyEntry.getSecretKey();
            } else {
                // Alias unavailable. Key not generated.
                final KeyGenerator keyGeneratorAES = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);
                final KeyGenParameterSpec.Builder keyGenParameterSpecBuilder = new KeyGenParameterSpec.Builder(
                        getAlias(),
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE);
                final KeyGenParameterSpec keyGenParameterSpec = keyGenParameterSpecBuilder.build();
                keyGeneratorAES.init(keyGenParameterSpec);
                return keyGeneratorAES.generateKey();
            }
        } catch (Exception e) {
            e.printStackTrace();
            ReportingManager.get().error(Gigya.VERSION, "core", "EncryptionException: unable to get/generate encryption key");
            throw new EncryptionException("GetKey: exception" + e.getMessage(), e.getCause());
        }
    }
}
