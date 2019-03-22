package com.gigya.android.sdk;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.encryption.IEncryptor;
import com.gigya.android.sdk.encryption.KeyStoreEncryptor;
import com.gigya.android.sdk.encryption.LegacyEncryptor;
import com.gigya.android.sdk.model.account.GigyaAccount;
import com.gigya.android.sdk.services.AccountService;
import com.gigya.android.sdk.services.ApiService;
import com.gigya.android.sdk.services.Config;
import com.gigya.android.sdk.services.PersistenceService;
import com.gigya.android.sdk.services.SessionService;
import com.gigya.android.sdk.services.SessionVerificationService;

/**
 * Gigya context specific service dependency holder/initiator.
 */
public class GigyaContext<A extends GigyaAccount> {

    /*
    SDK main configuration file.
     */
    @NonNull
    private Config _config = new Config();

    @NonNull
    public Config getConfig() {
        return _config;
    }

    public void setConfig(@NonNull Config config) {
        _config.updateWith(config);
    }

    /*
    Service for accessing shared preference persistence used mostly for session data encrypted persist.
     */
    private PersistenceService _persistenceService;

    public PersistenceService getPersistenceService() {
        return _persistenceService;
    }

    /*
    Service for storing cached account data and handling any account related logic.
     */
    private AccountService<A> _accountService;

    public AccountService<A> getAccountService() {
        return _accountService;
    }

    /*
    Service for handling session data & lifecycle.
     */
    private SessionService _sessionService;

    public SessionService getSessionService() {
        return _sessionService;
    }

    /*
    Encryption generator. Varies according to Android API level due to KeyStore backwards
    comparability issues.
     */
    private IEncryptor _encryptor;

    /*
    Service for initiating all API calls.
     */
    private ApiService<A> _apiService;

    public ApiService<A> getApiService() {
        return _apiService;
    }

    /*
    Session verification service. Used for periodic verifications of the current session.
     */
    private SessionVerificationService _sessionVerificationService;

    public SessionVerificationService getSessionVerificationService() {
        return _sessionVerificationService;
    }

    GigyaContext(Context appContext) {
        // Initialize all services.
        _persistenceService = new PersistenceService(appContext);
        _accountService = new AccountService<>();
        _encryptor = newEncryptor();
        Runnable newSessionRunnable = new Runnable() {
            @Override
            public void run() {
                if (_sessionVerificationService != null) {
                    // A new session has been updated. IF needed, start verification service.
                    _sessionVerificationService.start();
                }
            }
        };
        _sessionService = new SessionService(appContext, _config, _persistenceService, _encryptor, newSessionRunnable);
        _apiService = new ApiService<>(appContext, _sessionService, _accountService);
        _sessionVerificationService = new SessionVerificationService(appContext, _apiService);
    }

    /**
     * Generate a new Encryptor interface according to current Android OS version.
     *
     * @return Encryptor interface.
     */
    private IEncryptor newEncryptor() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 ? new KeyStoreEncryptor() : new LegacyEncryptor();
    }

    @Nullable
    <V> V getComponent(Class<V> type) {
        if (type == SessionService.class) {
            return type.cast(_sessionService);
        } else if (type == ApiService.class) {
            return type.cast(_apiService);
        } else if (type == PersistenceService.class) {
            return type.cast(_persistenceService);
        } else if (type == AccountService.class) {
            return type.cast(_accountService);
        } else {
            return null;
        }
    }

}
