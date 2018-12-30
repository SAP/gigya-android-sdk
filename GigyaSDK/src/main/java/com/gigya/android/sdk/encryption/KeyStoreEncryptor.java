package com.gigya.android.sdk.encryption;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.log.GigyaLogger;
import com.gigya.android.sdk.utils.CipherUtils;

import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Calendar;
import java.util.TimeZone;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;

/*
Session encryptor for Android devices with API >=18 which support KeyStore encrypted storage.
 */
public class KeyStoreEncryptor implements IEncryptor {

    private static final String LOG_TAG = "KeyStoreEncryptor";

    private static final String GS_PREFA_ALIAS = "GS_PREFA";

    private static final String ENCRYPTION_ALGORITHM = "RSA";
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String GS_KEYSTORE_ALIAS = "GS_ALIAS";
    private final String RSA_CIPHER = "RSA" + "/" + "ECB" + "/" + "PKCS1Padding";

    private KeyStore _keyStore;

    public KeyStoreEncryptor() throws EncryptionException {
        try {
            _keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            _keyStore.load(null);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new EncryptionException("Session encryption exception | exception loading keystore", ex.getCause());
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Nullable
    public SecretKey getKey(Context appContext, SharedPreferences prefs) throws EncryptionException {
        GigyaLogger.debug(LOG_TAG, "getKey: ");
        try {
            if (!_keyStore.containsAlias(GS_KEYSTORE_ALIAS)) {

                /*
                Generate KeyStore reference (Cert, private/public/secret).
                 */

                final KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ENCRYPTION_ALGORITHM, ANDROID_KEY_STORE);
                KeyPairGeneratorSpec spec = null;
                KeyGenParameterSpec specM = null;

                final TimeZone timeZone = TimeZone.getTimeZone("UTC");
                final Calendar start = Calendar.getInstance(timeZone);
                final Calendar end = Calendar.getInstance(timeZone);
                end.add(Calendar.YEAR, 25);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    specM = new KeyGenParameterSpec.Builder(
                            GS_KEYSTORE_ALIAS,
                            KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                            .setBlockModes(KeyProperties.BLOCK_MODE_ECB)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                            .build();

                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    spec = new KeyPairGeneratorSpec.Builder(appContext)
                            .setAlias(GS_KEYSTORE_ALIAS)
                            .setKeySize(2048)
                            .setKeyType(KeyProperties.KEY_ALGORITHM_RSA)
                            .setEndDate(end.getTime())
                            .setStartDate(start.getTime())
                            .setSerialNumber(BigInteger.ONE)
                            .setSubject(new X500Principal("CN = Secured Preference Store, O = Devliving Online"))
                            .build();
                } else {
                    spec = new KeyPairGeneratorSpec.Builder(appContext)
                            .setAlias(GS_KEYSTORE_ALIAS)
                            .setEndDate(end.getTime())
                            .setStartDate(start.getTime())
                            .setSerialNumber(BigInteger.ONE)
                            .setSubject(new X500Principal("CN = Secured Preference Store, O = Devliving Online"))
                            .build();
                }

                // Generate RSA keypair
                keyGen.initialize(spec == null ? specM : spec);
                keyGen.generateKeyPair();

                final PublicKey publicKey = _keyStore.getCertificate(GS_KEYSTORE_ALIAS).getPublicKey();

                // Generate AES secret key
                final KeyGenerator generator = KeyGenerator.getInstance("AES");
                generator.init(128); // The AES key size in number of bits
                final SecretKey secretKey = generator.generateKey();

                // Cipher AES key with RSA
                final Cipher cipher = Cipher.getInstance(RSA_CIPHER);
                cipher.init(Cipher.ENCRYPT_MODE, publicKey);

                byte[] encryptedAES = cipher.doFinal(secretKey.getEncoded());
                // Save encrypted AES to sharedPreferences
                final String newEncryptedSecret = CipherUtils.bytesToString(encryptedAES);
                prefs.edit().putString(GS_PREFA_ALIAS, newEncryptedSecret).apply();
                return secretKey;

            } else if (!_keyStore.entryInstanceOf(GS_KEYSTORE_ALIAS, KeyStore.PrivateKeyEntry.class)) {

                /*
                Determines if the keystore for the specified entry is an instance or subclass of the alias specified.
                IF not delete the entry and create a new one.
                 */
                _keyStore.deleteEntry(GS_KEYSTORE_ALIAS);
                return getKey(appContext, prefs);

            } else {

                /*
                Keystore instance exist. Load keys and decrypt secret.
                 */

                String aesKey = prefs.getString(GS_PREFA_ALIAS, null);
                if (aesKey != null && _keyStore.containsAlias(GS_KEYSTORE_ALIAS) && _keyStore.entryInstanceOf(GS_KEYSTORE_ALIAS, KeyStore.PrivateKeyEntry.class)) {
                    final PrivateKey privateKey = (PrivateKey) _keyStore.getKey(GS_KEYSTORE_ALIAS, null);
                    final Cipher cipher = Cipher.getInstance(RSA_CIPHER);
                    cipher.init(Cipher.DECRYPT_MODE, privateKey);
                    // Decrypt secret key.
                    byte[] decryptedAES = cipher.doFinal(CipherUtils.stringToBytes(aesKey));
                    return new SecretKeySpec(decryptedAES, 0, decryptedAES.length, "AES");
                }
                return null;
            }
        } catch (Exception ex) {
            throw new EncryptionException("Session encryption exception\n" + ex.getLocalizedMessage(), ex.getCause());
        }
    }

}
