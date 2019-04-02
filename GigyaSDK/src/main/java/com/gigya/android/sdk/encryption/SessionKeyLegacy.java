package com.gigya.android.sdk.encryption;

import com.gigya.android.sdk.persistence.IPersistenceService;
import com.gigya.android.sdk.utils.CipherUtils;

import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class SessionKeyLegacy implements ISecureKey {

    private final IPersistenceService _psService;

    public SessionKeyLegacy(IPersistenceService psService) {
        _psService = psService;
    }

    @Override
    public String getAlias() {
        return null; // Not needed as legacy does not support keystore.
    }

    @Override
    public String getTransformation() {
        return null; // Not needed as legacy does not support keystore.
    }

    @Override
    public Cipher getEncryptionCipher(Key key) throws EncryptionException {
        return null;
    }

    @Override
    public Cipher getDecryptionCipher(Key key) throws EncryptionException {
        return null;
    }

    @Override
    public SecretKey getKey() throws EncryptionException {
        try {
            final String SECRET_PREFERENCE_KEY = "GS_PREFA";
            final String ALGORITHM_KEY = "AES";

            final String encryptedSecret = _psService.getString(SECRET_PREFERENCE_KEY, null);
            if (encryptedSecret != null) {
                // Secret key is saved in the shared preferences as a byte array.
                final byte[] decryptedAES = CipherUtils.stringToBytes(encryptedSecret);
                return new SecretKeySpec(decryptedAES, 0, decryptedAES.length, ALGORITHM_KEY);
            }
            // Generate a new secret key.
            final KeyGenerator generator = KeyGenerator.getInstance(ALGORITHM_KEY);
            generator.init(128); // The AES key size in number of bits
            final SecretKey secretKey = generator.generateKey();
            final String newEncryptedSecret = CipherUtils.bytesToString(secretKey.getEncoded());
            _psService.add(SECRET_PREFERENCE_KEY, newEncryptedSecret);
            return secretKey;
        } catch (Exception ex) {
            throw new EncryptionException("Session encryption exception", ex.getCause());
        }
    }
}
