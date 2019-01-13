package com.gigya.android.sdk.encryption;

import android.content.Context;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.PersistenceHandler;
import com.gigya.android.sdk.log.GigyaLogger;
import com.gigya.android.sdk.utils.CipherUtils;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/*
Session encryptor for Legacy Android devices spanning from API <=17.
 */
public class LegacyEncryptor implements IEncryptor {

    private static final String LOG_TAG = "LegacyEncryptor";

    private static final String GS_PREFA_ALIAS = "GS_PREFA";

    private static final String ENCRYPTION_ALGORITHM = "AES";

    @Override
    @Nullable
    public SecretKey getKey(Context appContext, PersistenceHandler persistenceHandler) throws EncryptionException {
        GigyaLogger.debug(LOG_TAG, "getKey: ");
        try {
            final String encryptedSecret = persistenceHandler.getString(GS_PREFA_ALIAS, null);
            if (encryptedSecret != null) {

                /*
                Secret available from shared preferences.
                 */

                final byte[] decryptedAES = CipherUtils.stringToBytes(encryptedSecret);
                return new SecretKeySpec(decryptedAES, 0, decryptedAES.length, ENCRYPTION_ALGORITHM);
            }

            /*
            Generate a new secret instance.
             */

            final KeyGenerator generator = KeyGenerator.getInstance(ENCRYPTION_ALGORITHM);
            generator.init(128); // The AES key size in number of bits
            final SecretKey secretKey = generator.generateKey();
            final String newEncryptedSecret = CipherUtils.bytesToString(secretKey.getEncoded());
            persistenceHandler.add(GS_PREFA_ALIAS, newEncryptedSecret);
            return secretKey;
        } catch (Exception ex) {
            throw new EncryptionException("Session encryption exception", ex.getCause());
        }
    }

}
