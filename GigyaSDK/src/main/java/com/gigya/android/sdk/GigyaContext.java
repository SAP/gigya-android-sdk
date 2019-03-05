package com.gigya.android.sdk;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;

import com.gigya.android.sdk.encryption.IEncryptor;
import com.gigya.android.sdk.encryption.KeyStoreEncryptor;
import com.gigya.android.sdk.encryption.LegacyEncryptor;
import com.gigya.android.sdk.model.account.GigyaAccount;
import com.gigya.android.sdk.services.AccountService;
import com.gigya.android.sdk.services.ApiService;
import com.gigya.android.sdk.services.Config;
import com.gigya.android.sdk.services.PersistenceService;
import com.gigya.android.sdk.services.SessionService;

/**
 * Gigya context specific service dependency holder/initiator.
 */
public class GigyaContext<T extends GigyaAccount> {

    /*
    SDK main configuration file.
     */
    @NonNull
    private Config _config = new Config();

    /*
    Service for accessing shared preference persistence used mostly for session data encrypted persist.
     */
    private PersistenceService _persistenceService;

    /*
    Service for storing cached account data and handling any account related logic.
     */
    private AccountService<T> _accountService;

    /*
    Service for handling session data & lifecycle.
     */
    private SessionService _sessionService;

    /*
    Encryption generator. Varies according to Android API level due to KeyStore backwards
    comparability issues.
     */
    private IEncryptor _encryptor;

    /*
    Service for initiating all API calls.
     */
    private ApiService<T> _apiService;


    public GigyaContext(Context appContext) {
        // Initialize all services.
        _persistenceService = new PersistenceService(appContext);
        _accountService = new AccountService<>();
        _encryptor = newEncryptor();
        _sessionService = new SessionService(appContext, _config, _persistenceService, _encryptor);
        _apiService = new ApiService<>(appContext, _sessionService, _accountService);
    }

    /**
     * Generate a new Encryptor interface according to current Android OS version.
     *
     * @return Encryptor interface.
     */
    private IEncryptor newEncryptor() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 ? new KeyStoreEncryptor() : new LegacyEncryptor();
    }
}
