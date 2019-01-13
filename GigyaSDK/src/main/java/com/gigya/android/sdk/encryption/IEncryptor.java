package com.gigya.android.sdk.encryption;

import android.content.Context;

import com.gigya.android.sdk.PersistenceHandler;

import javax.crypto.SecretKey;

public interface IEncryptor {

    SecretKey getKey(Context appContext, PersistenceHandler persistenceHandler) throws EncryptionException;
}
