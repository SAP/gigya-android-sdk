package com.gigya.android.sdk.biometric;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.encryption.EncryptionException;
import com.gigya.android.sdk.encryption.ISecureKey;
import com.gigya.android.sdk.persistence.IPersistenceService;
import com.gigya.android.sdk.persistence.PersistenceService;

import java.security.Key;
import java.security.KeyStore;
import java.util.Enumeration;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class BiometricKey implements ISecureKey {

    private static final String LOG_TAG = "BiometricKey";

    private final IPersistenceService _psService;

    public BiometricKey(IPersistenceService pService) {
        _psService = pService;
    }

    @Override
    public String getAlias() {
        return "fingerprint";
    }

    @Override
    public String getTransformation() {
        return "AES/CBC/PKCS7Padding";
    }

    @Override
    public Cipher getEncryptionCipher(Key key) throws EncryptionException {
        try {
            Cipher cipher = Cipher.getInstance(getTransformation());
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return cipher;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new EncryptionException("getEncryptionCipher: exception" + ex.getMessage(), ex.getCause());
        }
    }

    @Override
    public Cipher getDecryptionCipher(Key key) throws EncryptionException {
        try {
            Cipher cipher = Cipher.getInstance(getTransformation());
            final String ivSpec = _psService.getString(PersistenceService.PREFS_KEY_IV_SPEC, null);
            if (ivSpec != null) {
                final IvParameterSpec spec = new IvParameterSpec(Base64.decode(ivSpec, Base64.DEFAULT));
                cipher.init(Cipher.DECRYPT_MODE, key, spec);
                return cipher;
            } else {
                GigyaLogger.error(LOG_TAG, "createCipherFor: getIVSpec null");
                return null;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new EncryptionException("getDecryptionCipher: exception" + ex.getMessage(), ex.getCause());
        }
    }

    @Override
    public SecretKey getKey() throws EncryptionException {
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);

            final Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                if (getAlias().equals(aliases.nextElement())) {
                    try {
                        return (SecretKey) keyStore.getKey(getAlias(), null);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        deleteKey();
                    }
                }
            }
            final KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            keyGenerator.init(new
                    KeyGenParameterSpec.Builder(getAlias(),
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .setUserAuthenticationRequired(true)
                    .setKeySize(256)
                    .build());
            return keyGenerator.generateKey();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Delete fingerprint KeyStore if exists.
     */
    public void deleteKey() {
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.deleteEntry(getAlias());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
