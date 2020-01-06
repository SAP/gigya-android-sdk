package com.gigya.android.sdk.encryption;

import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

public interface ISecureKey {

    String getAlias();

    String getTransformation();

    Cipher getEncryptionCipher(Key key) throws EncryptionException;

    Cipher getDecryptionCipher(Key key) throws EncryptionException;

    SecretKey getKey() throws EncryptionException;
}
