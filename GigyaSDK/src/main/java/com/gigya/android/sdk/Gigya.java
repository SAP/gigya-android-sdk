package com.gigya.android.sdk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import com.gigya.android.sdk.api.LogoutApi;
import com.gigya.android.sdk.api.RefreshProviderSessionApi;
import com.gigya.android.sdk.api.RegisterApi;
import com.gigya.android.sdk.log.GigyaLogger;
import com.gigya.android.sdk.login.LoginProvider;
import com.gigya.android.sdk.login.LoginProviderFactory;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.model.GigyaAccount;
import com.gigya.android.sdk.model.SessionInfo;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.GigyaResponse;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;
import com.gigya.android.sdk.ui.GigyaLoginPresenter;

import java.util.HashMap;
import java.util.Map;

public class Gigya<T extends GigyaAccount> {

    private static final String LOG_TAG = "Gigya";

    public static final String VERSION = "android_4.0.0";

    @SuppressLint("StaticFieldLeak")
    private static Gigya _sharedInstance;

    @NonNull
    final private Context _appContext;

    @NonNull
    public Context getContext() {
        return _appContext;
    }

    private Gigya(@NonNull Context appContext) {
        _appContext = appContext;
        init();
        _sessionManager = new SessionManager(this,
                DependencyRegistry.getInstance().getEncryptor(),
                DependencyRegistry.getInstance().getPersistenceHandler(_appContext));
        if (_currentProvider != null) {
            /* Track provider token changes if necessary. */
            _currentProvider.trackTokenChanges(_sessionManager);
        }
    }

    private NetworkAdapter _networkAdapter;

    /*
    Simplified instance getter for use only after calling getInstance(Context context) at least once.
     */
    @SuppressWarnings("unchecked")
    public static synchronized Gigya<GigyaAccount> getInstance() {
        if (_sharedInstance == null) {
            // Log error.
            // TODO: 10/12/2018 Error logs will need pronunciation review from product
            GigyaLogger.error(LOG_TAG, "Gigya instance not initialized properly!" +
                    " Make sure to call Gigya getInstance(Context appContext) at least once before trying to reference The Gigya instance");
            return null;
        }
        return _sharedInstance;
    }

    /*
    Simplified instance getter.
     */
    public static synchronized Gigya<GigyaAccount> getInstance(Context appContext) {
        return Gigya.getInstance(appContext, GigyaAccount.class);
    }

    //region Initialize

    /**
     * Gigya default api domain.
     */
    private static final String DEFAULT_API_DOMAIN = "us1.gigya.com";

    /*
    Base SDK configuration model (contains Api-Key, Api-domain & ids).
     */
    private Configuration _configuration = new Configuration();

    @NonNull
    public Configuration getConfiguration() {
        return _configuration;
    }

    /*
    Generic account type instance getter.
     */
    @SuppressWarnings("unchecked")
    public static synchronized <V extends GigyaAccount> Gigya<V> getInstance(Context appContext, @NonNull Class<V> accountClazz) {
        if (_sharedInstance == null) {
            _sharedInstance = new Gigya(appContext);
        }
        _sharedInstance._accountManager.setAccountClazz(accountClazz);
        return _sharedInstance;
    }

    /**
     * Explicitly initialize the SDK.
     * Using this init() method will set the SDK domain to the default "us1.gigya.com"
     * see {@link #init(String, String)} to explicitly set the required domain.
     *
     * @param apiKey Client API-KEY.
     */
    @SuppressWarnings("unused")
    public void init(String apiKey) {
        init(apiKey, DEFAULT_API_DOMAIN);
    }

    /**
     * Explicitly initialize the SDK.
     *
     * @param apiKey    Client API-KEY
     * @param apiDomain Request Domain.
     */
    @SuppressWarnings("WeakerAccess")
    public void init(String apiKey, String apiDomain) {
        // Override existing configuration when applied explicitly.
        _configuration.update(apiKey, apiDomain);
        init();
    }

    /**
     * Implicitly initialize the SDK.
     * Available Options:
     * - read JSON assets file.
     * - parse application manifest meta data tags.
     * For explicit setting see {@link #init(String, String)} method.
     */
    private void init() {
        if (_configuration.getApiKey() == null) {
            /* Try to from assets JSON file, */
            _configuration = Configuration.loadFromJson(_appContext);
            if (_configuration == null) {
                /* Try to load fom manifest meta data. */
                _configuration = Configuration.loadFromManifest(_appContext);
            }
        }

        /* Set next account invalidation timestamp if available. */
        if (_configuration != null && _configuration.getAccountCacheTime() != 0) {
            _accountManager.setAccountCacheTime(_configuration.getAccountCacheTime());
            _accountManager.nextAccountInvalidationTimestamp();
        }

        /* Load last provider if exists. */
        final String lastProviderName = DependencyRegistry.getInstance().getPersistenceHandler(_appContext)
                .getString("lastLoginProvider", null);
        if (lastProviderName != null) {
            _currentProvider = LoginProviderFactory.providerFor(_appContext, _configuration, lastProviderName, null, _loginTrackerCallback);
            if (_currentProvider.clientIdRequired()) {
                /* Must call sdk config to fetch related client ids for login provider. */
                loadSDKConfig(new Runnable() {
                    @Override
                    public void run() {
                        if (!_configuration.getAppIds().isEmpty() && _currentProvider != null) {
                            final String providerClientId = _configuration.getAppIds().get(lastProviderName);
                            if (providerClientId != null) {
                                _currentProvider.updateProviderClientId(providerClientId);
                            }
                        }
                    }
                });
            }
        }
    }

    //endregion

    //region Networking

    private NetworkAdapter getNetworkAdapter() {
        if (_networkAdapter == null) {
            _networkAdapter = new NetworkAdapter(_appContext, new NetworkAdapter.IConfigurationBlock() {
                @Override
                public void onMissingConfiguration() {
                    if (!_configuration.hasGMID()) {
                        loadSDKConfig(null);
                    }
                }
            });
        }
        return _networkAdapter;
    }

    private ApiManager<T> _apiManager;

    private ApiManager<T> getApiManager() {
        if (_apiManager == null) {
            _apiManager = new ApiManager<>(_configuration, getNetworkAdapter(), _sessionManager, _accountManager);
        }
        return _apiManager;
    }

    /*
   Request SDK configuration. Crucial -> fetches GMID fields needed for all requests.
    */
    private void loadSDKConfig(final Runnable completionHandler) {
        GigyaLogger.debug(LOG_TAG, "api: socialize.getSDKConfig queued execute");
        getApiManager().loadConfig(completionHandler);
    }

    /**
     * Send request to Gigya servers.
     *
     * @param api      Request method identifier.
     * @param params   Additional parameters.
     * @param callback Response listener callback.
     */
    @SuppressWarnings("unchecked")
    public void send(String api, Map<String, Object> params, GigyaCallback<GigyaResponse> callback) {
        getApiManager().sendAnonymous(api, params, callback);
    }

    /**
     * Send request to Gigya servers.
     *
     * @param api      Request method identifier.
     * @param params   Additional parameters.
     * @param clazz    Response class scheme.
     * @param callback Response listener callback.
     */
    @SuppressWarnings("unchecked")
    public <H> void send(String api, Map<String, Object> params, Class<H> clazz, GigyaCallback<H> callback) {
        getApiManager().sendAnonymous(api, params, clazz, callback);
    }

    //endregion

    //region GigyaAccount & Session

    private AccountManager<T> _accountManager = DependencyRegistry.getInstance().getAccountManager();

    @Nullable
    private LoginProvider _currentProvider;

    @Nullable
    public LoginProvider getLoginProvider() {
        return _currentProvider;
    }

    @Nullable
    private SessionManager _sessionManager;

    /**
     * Get current session.
     *
     * @return SessionInfo instance.
     */
    @Nullable
    public SessionInfo getSession() {
        if (_sessionManager == null) {
            return null;
        }
        return _sessionManager.getSession();
    }

    /**
     * Check if we currently have a valid session.
     */
    public boolean isLoggedIn() {
        return _sessionManager != null && _sessionManager.isValidSession();
    }

    /**
     * Logout of Gigya services.
     * This will clean all session related data persistence.
     */
    public void logout() {
        new LogoutApi(_configuration, getNetworkAdapter(), _sessionManager).call();
        if (_sessionManager != null) {
            _sessionManager.clear();
        }
        getNetworkAdapter().cancel(null);
        GigyaLoginPresenter.flush();

        /* Clearing cached cookies. */
        CookieSyncManager.createInstance(_appContext);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();

        /* Logout from social provider (is available). */
        if (_currentProvider != null) {
            _currentProvider.logout(_appContext);
        }

        /* Persistence related logout tasks. */
        DependencyRegistry.getInstance().getPersistenceHandler(_appContext).onLogout();
    }

    //endregion

    //region Business Apis

    /**
     * Login with provided id & password.
     *
     * @param username Login username.
     * @param password Login password.
     * @param callback Response listener callback.
     */
    public void login(String username, String password, GigyaCallback<T> callback) {
        getApiManager().login(username, password, callback);
    }

    /**
     * Request account info.
     *
     * @param callback Response listener callback.
     */
    public void getAccount(GigyaCallback<T> callback) {
        getApiManager().getAccount(callback);
    }

    /**
     * Request account info.
     *
     * @param overrideCache Should override the account caching option. When set to true, the SDK will not cache the account object.
     * @param callback      Response listener callback.
     */
    @SuppressWarnings("unused")
    public void getAccount(final boolean overrideCache, GigyaCallback<T> callback) {
        _accountManager.setAccountOverrideCache(overrideCache);
        getAccount(callback);
    }

    /**
     * Set account info
     *
     * @param account  Updated account object.
     * @param callback Response listener callback.
     */
    public void setAccount(T account, GigyaCallback<T> callback) {
        getApiManager().setAccount(account, callback);
    }

    /**
     * Register account using email & password combination.
     *
     * @param email    User email identifier.
     * @param password User password.
     * @param callback Response listener callback.
     */
    @SuppressWarnings("unused")
    public void register(String email, String password, GigyaRegisterCallback<T> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("loginID", email);
        params.put("password", password);
        register(params, RegisterApi.RegisterPolicy.EMAIL, true, callback);
    }

    /**
     * Register account using login id, password & required login policy identifier.
     *
     * @param loginID  User loginID (can be email, username. According to site policy).
     * @param password User password.
     * @param policy   Login policy policy {@link RegisterApi.RegisterPolicy}
     * @param finalize Finalize registration.
     * @param callback Response listener callback.
     */
    public void register(String loginID, String password, RegisterApi.RegisterPolicy policy, boolean finalize, GigyaRegisterCallback<T> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("loginID", loginID);
        params.put("password", password);
        register(params, policy, finalize, callback);
    }

    /* Private initiator. */
    private void register(Map<String, Object> params, RegisterApi.RegisterPolicy policy, boolean finalize, GigyaRegisterCallback<T> callback) {
        getApiManager().register(params, policy, finalize, callback);
    }

    //endregion

    //region Native login

    /*
    Token tracker callback. Shared between all providers (if needed).
     */
    private LoginProvider.LoginProviderTrackerCallback _loginTrackerCallback = new LoginProvider.LoginProviderTrackerCallback() {
        @Override
        public void onProviderTrackingTokenChanges(String provider, String providerSession, final LoginProvider.LoginPermissionCallbacks permissionCallbacks) {
            GigyaLogger.debug(LOG_TAG, "onProviderTrackingTokenChanges: provider = "
                    + provider + ", providerSession =" + providerSession);

            /* Refresh session token. */
            new RefreshProviderSessionApi(_configuration, getNetworkAdapter(), _sessionManager)
                    .call(providerSession, new GigyaCallback() {
                        @Override
                        public void onSuccess(Object obj) {
                            GigyaLogger.debug(LOG_TAG, "onProviderTrackingTokenChanges: Success - provider token updated");

                            if (permissionCallbacks != null) {
                                permissionCallbacks.granted();
                            }
                            /* Invalidate cached account. */
                            _accountManager.invalidateAccount();
                        }

                        @Override
                        public void onError(GigyaError error) {
                            GigyaLogger.debug(LOG_TAG, "onProviderTrackingTokenChanges: Error: " + error.getLocalizedMessage());

                            if (permissionCallbacks != null) {
                                permissionCallbacks.failed(error.getLocalizedMessage());
                            }
                        }
                    });
        }
    };

    /**
     * Present native login selection according to requested parameters.
     *
     * @param params   Requested parameters.
     * @param callback Response listener callback.
     */
    public void loginWithSelectedLoginProviders(final Map<String, Object> params, final GigyaCallback<T> callback) {
        new GigyaLoginPresenter(getApiManager(), DependencyRegistry.getInstance().getPersistenceHandler(_appContext))
                .showNativeLoginProviders(_appContext, _configuration, params, _loginTrackerCallback, new GigyaLoginPresenter.LoginPresentationCallbacks() {
                    @Override
                    public void onProviderSelected(LoginProvider loginProvider) {
                        /* Update current provider. */
                        _currentProvider = loginProvider;
                        _currentProvider.trackTokenChanges(_sessionManager);
                    }

                    @Override
                    public void onCancelled() {
                        callback.onCancelledOperation();
                    }

                }, callback);
    }

    //endregion
}
