package com.gigya.android.sdk.encryption;

import android.content.Context;

import com.gigya.android.sdk.PersistenceManager;

import javax.crypto.SecretKey;

public interface IEncryptor {

    SecretKey getKey(Context appContext, PersistenceManager persistenceManager) throws EncryptionException;
}
