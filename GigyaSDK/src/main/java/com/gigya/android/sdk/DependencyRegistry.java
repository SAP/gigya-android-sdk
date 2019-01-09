package com.gigya.android.sdk;

import android.os.Build;

import com.gigya.android.sdk.encryption.IEncryptor;
import com.gigya.android.sdk.encryption.KeyStoreEncryptor;
import com.gigya.android.sdk.encryption.LegacyEncryptor;

class DependencyRegistry {

    private static DependencyRegistry _sharedInstance;

    private DependencyRegistry() {

    }

    public static DependencyRegistry getInstance() {
        if (_sharedInstance == null) {
            _sharedInstance = new DependencyRegistry();
        }
        return _sharedInstance;
    }

    //region Encryption helper

    private IEncryptor _encryptor;

    IEncryptor getEncryptor() {
        if (_encryptor == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                _encryptor = new KeyStoreEncryptor();
            } else {
                _encryptor = new LegacyEncryptor();
            }
        }
        return _encryptor;
    }

    //endregion
}
