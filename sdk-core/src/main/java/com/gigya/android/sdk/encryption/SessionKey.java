package com.gigya.android.sdk.encryption;

import android.content.Context;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import androidx.annotation.RequiresApi;

import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.persistence.IPersistenceService;
import com.gigya.android.sdk.reporting.ReportingManager;
import com.gigya.android.sdk.utils.CipherUtils;

import java.math.BigInteger;
import java.security.Key;
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

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class SessionKey implements ISecureKey {

    private String TYPE = "AndroidKeyStore";

    private final Context _context;
    private final IPersistenceService _psService;

    final static String SECRET_PREFERENCE_KEY = "GS_PREFA";

    public SessionKey(Context context, IPersistenceService psService) {
        _context = context;
        _psService = psService;
    }

    @Override
    public String getAlias() {
        return "GS_ALIAS";
    }

    @Override
    public String getTransformation() {
        return "RSA/ECB/PKCS1Padding"; // Not using constants because they are only available from >=23.
    }

    @Override
    public Cipher getEncryptionCipher(Key key) throws EncryptionException {
        try {
            final Cipher cipher = Cipher.getInstance(getTransformation());
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return cipher;
        } catch (Exception ex) {
            ex.printStackTrace();
            ReportingManager.get().error(Gigya.VERSION, "core", "getDecryptionCipher error");
            throw new EncryptionException("getDecryptionCipher: exception" + ex.getMessage(), ex.getCause());
        }
    }

    @Override
    public Cipher getDecryptionCipher(Key key) throws EncryptionException {
        try {
            final Cipher cipher = Cipher.getInstance(getTransformation());
            cipher.init(Cipher.DECRYPT_MODE, key);
            return cipher;
        } catch (Exception ex) {
            ex.printStackTrace();
            ReportingManager.get().error(Gigya.VERSION, "core", "getDecryptionCipher error");
            throw new EncryptionException("getDecryptionCipher: exception" + ex.getMessage(), ex.getCause());
        }
    }


    @Override
    public SecretKey getKey() throws EncryptionException {
        try {
            KeyStore keyStore = KeyStore.getInstance(TYPE);
            keyStore.load(null);
            if (!keyStore.containsAlias(getAlias())) { // Keystore not available.
                // Generate certificate for this new alias.
                generateCertificateForAlias();
                final PublicKey publicKey = keyStore.getCertificate(getAlias()).getPublicKey();
                // Generate AES secret key
                final KeyGenerator generator = KeyGenerator.getInstance("AES");
                generator.init(128);
                final SecretKey secretKey = generator.generateKey();
                // Encrypt secret key and save it.
                final Cipher cipher = getEncryptionCipher(publicKey);
                byte[] encryptedAES = cipher.doFinal(secretKey.getEncoded());
                final String newEncryptedSecret = CipherUtils.bytesToString(encryptedAES);
                _psService.add(SECRET_PREFERENCE_KEY, newEncryptedSecret);
                return secretKey;
            } else if (!keyStore.entryInstanceOf(getAlias(), KeyStore.PrivateKeyEntry.class)) {
                // Determines if the keystore for the specified entry is an instance or subclass of the alias specified.
                // If not delete the entry and create a new one.
                keyStore.deleteEntry(getAlias());
                return getKey(); // Recursive.
            } else {
                // Keystore instance exist. Get private key from KeyStore instance and decrypt & generate the secret key.

                final String aesKey = _psService.getString(SECRET_PREFERENCE_KEY, null);
                if (aesKey != null && keyStore.containsAlias(getAlias()) && keyStore.entryInstanceOf(getAlias(), KeyStore.PrivateKeyEntry.class)) {
                    // In (v3) secret key is ciphered and saved in the shared preferences.
                    final PrivateKey privateKey = (PrivateKey) keyStore.getKey(getAlias(), null);
                    final Cipher cipher = getDecryptionCipher(privateKey);
                    byte[] decrypted = cipher.doFinal(CipherUtils.stringToBytes(aesKey));
                    final String ALGORITHM_KEY = "AES";
                    return new SecretKeySpec(decrypted, 0, decrypted.length, ALGORITHM_KEY);
                }
                return null;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            ReportingManager.get().error(Gigya.VERSION, "core", "EncryptionException: unable to get/generate encryption key");
            throw new EncryptionException("GetKey: exception" + ex.getMessage(), ex.getCause());
        }
    }

    //region CERTIFICATE

    private void generateCertificateForAlias() throws Exception {
        final String ALGORITHM_KEYSTORE = "RSA";
        final KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM_KEYSTORE, TYPE);
        final TimeZone timeZone = TimeZone.getTimeZone("UTC");
        final Calendar start = Calendar.getInstance(timeZone);
        final Calendar end = Calendar.getInstance(timeZone);
        end.add(Calendar.YEAR, 25);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                    getAlias(),
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                    .setBlockModes(KeyProperties.BLOCK_MODE_ECB)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                    .setKeyValidityStart(start.getTime())
                    .setKeyValidityEnd(end.getTime())
                    .build();
            keyGen.initialize(spec);
            keyGen.generateKeyPair();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            final KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(_context)
                    .setAlias(getAlias())
                    .setKeySize(2048)
                    .setEndDate(end.getTime())
                    .setStartDate(start.getTime())
                    .setSerialNumber(BigInteger.ONE)
                    .setSubject(new X500Principal("CN = Secured Preference Store, O = Devliving Online"))
                    .build();
            keyGen.initialize(spec);
            keyGen.generateKeyPair();
        } else {
            final KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(_context)
                    .setAlias(getAlias())
                    .setEndDate(end.getTime())
                    .setStartDate(start.getTime())
                    .setSerialNumber(BigInteger.ONE)
                    .setSubject(new X500Principal("CN = Secured Preference Store, O = Devliving Online"))
                    .build();
            keyGen.initialize(spec);
            keyGen.generateKeyPair();
        }
    }

    //endregion

}
