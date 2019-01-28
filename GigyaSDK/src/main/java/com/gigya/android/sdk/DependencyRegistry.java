package com.gigya.android.sdk;

import android.content.Context;
import android.os.Build;

import com.gigya.android.sdk.encryption.IEncryptor;
import com.gigya.android.sdk.encryption.KeyStoreEncryptor;
import com.gigya.android.sdk.encryption.LegacyEncryptor;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;
import com.gigya.android.sdk.ui.WebBridge;

public class DependencyRegistry {

    private static DependencyRegistry _sharedInstance;

    private DependencyRegistry() {
    }

    public static DependencyRegistry getInstance() {
        if (_sharedInstance == null) {
            _sharedInstance = new DependencyRegistry();
        }
        return _sharedInstance;
    }

    //region Dependencies

    private Configuration _configuration = new Configuration();
    private IEncryptor _encryptor;
    private PersistenceManager _persistenceManager;
    private NetworkAdapter _networkAdapter;
    private ApiManager _apiManager;
    private SessionManager _sessionManager;
    private AccountManager _accountManager;

    /**
     * @param appContext Application context.
     * @param <T>        Account scheme
     */
    public <T> void init(Context appContext) {
        // Persistence manager.
        _persistenceManager = new PersistenceManager(appContext);
        // Initialize encryptor.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            _encryptor = new KeyStoreEncryptor();
        } else {
            _encryptor = new LegacyEncryptor();
        }
        // Network adapter.
        _networkAdapter = new NetworkAdapter(appContext, new NetworkAdapter.IConfigurationBlock() {
            @Override
            public void onMissingConfiguration() {
                if (!DependencyRegistry.getInstance()._configuration.hasGMID()) {
                    _apiManager.loadConfig(null);
                }
            }
        });
        _accountManager = new AccountManager<T>();
        _sessionManager = new SessionManager(appContext);
        // Api manager
        _apiManager = new ApiManager<T>();
    }

    //endregion

    //region Setters

    public void setConfiguration(Configuration configuration) {
        _configuration.update(configuration);
    }

    //endregion

    //region Getters

    public Configuration getConfiguration() {
        return _configuration;
    }

    public SessionManager getSessionManager() {
        return _sessionManager;
    }

    public <T> AccountManager<T> getAccountManager() {
        return _accountManager;
    }

    public ApiManager getApiManager() {
        return _apiManager;
    }

    public NetworkAdapter getNetworkAdapter() {
        return _networkAdapter;
    }

    public PersistenceManager getPersistenceManager() {
        return _persistenceManager;
    }

    public IEncryptor getEncryptor() {
        return _encryptor;
    }

    //endregion

    //region Injections

    public void inject(WebBridge webBridge) {
        webBridge.inject(getConfiguration(), getSessionManager());
    }

    public void inject(SessionManager sessionManager) {
        sessionManager.inject(getConfiguration(), getEncryptor(), getPersistenceManager());
    }

    public <T> void inject(ApiManager<T> apiManager) {
        apiManager.inject(getConfiguration(), getNetworkAdapter(), getSessionManager(), this.<T>getAccountManager());
    }

    //endregion
}
