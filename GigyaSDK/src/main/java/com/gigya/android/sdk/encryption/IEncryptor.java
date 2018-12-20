package com.gigya.android.sdk.encryption;

import android.content.Context;
import android.content.SharedPreferences;

import javax.crypto.SecretKey;

public interface IEncryptor {

    SecretKey getKey(Context appContext, SharedPreferences prefs) throws EncryptionException;
}
