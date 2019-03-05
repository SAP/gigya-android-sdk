package com.gigya.android.sdk.encryption;

import android.content.Context;

import com.gigya.android.sdk.services.PersistenceService;

import javax.crypto.SecretKey;

public interface IEncryptor {

    SecretKey getKey(Context appContext, PersistenceService persistenceManager) throws EncryptionException;
}
