package com.gigya.android.sdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.log.GigyaLogger;
import com.gigya.android.sdk.login.LoginProvider;
import com.gigya.android.sdk.login.LoginProviderFactory;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.model.GigyaAccount;
import com.gigya.android.sdk.model.SessionInfo;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.GigyaInterceptionCallback;
import com.gigya.android.sdk.network.GigyaResponse;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;
import com.gigya.android.sdk.network.api.AnonymousApi;
import com.gigya.android.sdk.network.api.GetAccountApi;
import com.gigya.android.sdk.network.api.LoginApi;
import com.gigya.android.sdk.network.api.LogoutApi;
import com.gigya.android.sdk.network.api.NotifyLoginApi;
import com.gigya.android.sdk.network.api.RefreshProviderSessionApi;
import com.gigya.android.sdk.network.api.RegisterApi;
import com.gigya.android.sdk.network.api.SdkConfigApi;
import com.gigya.android.sdk.network.api.SetAccountApi;
import com.gigya.android.sdk.ui.GigyaPresenter;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

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

    //region Initialization

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

    private boolean isValidConfiguration() {
        if (_configuration == null) {
            return false;
        }
        return _configuration.hasApiKey();
    }

    /*
    Generic account type instance getter.
     */
    @SuppressWarnings("unchecked")
    public static synchronized <V extends GigyaAccount> Gigya<V> getInstance(Context appContext, @NonNull Class<V> accountClazz) {
        if (_sharedInstance == null) {
            _sharedInstance = new Gigya(appContext);
        }
        _sharedInstance._accountClazz = accountClazz;
        return _sharedInstance;
    }

    /**
     * Explicitly initialize the SDK.
     * Using this init() method will set the SDK domain to the default "us1.gigya.com"
     * see {@link #init(String, String)} to explicitly set the required domain.
     *
     * @param apiKey Client API-KEY.
     */
    public void init(String apiKey) {
        init(apiKey, DEFAULT_API_DOMAIN);
    }

    /**
     * Explicitly initialize the SDK.
     *
     * @param apiKey    Client API-KEY
     * @param apiDomain Request Domain.
     */
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
            _accountInvalidationTimestamp = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(_configuration.getAccountCacheTime());
        }

        /* Load last provider if exists. */
        final String lastProviderName = DependencyRegistry.getInstance().getPersistenceHandler(_appContext)
                .getString("lastLoginProvider", null);
        if (lastProviderName != null) {
            _currentProvider = LoginProviderFactory.providerFor(_appContext, lastProviderName, null, _loginTrackerCallback);
            if (_currentProvider != null && _currentProvider.clientIdRequired()) {
                /* Must call sdk config to fetch related client ids for login provider. */
                getSdkConfig(new Runnable() {
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
                        getSdkConfig(null);
                    }
                }
            });
        }
        return _networkAdapter;
    }


    /*
   Request SDK configuration. Crucial -> fetches GMID fields needed for all requests.
    */
    private void getSdkConfig(final Runnable completionHandler) {
        GigyaLogger.debug(LOG_TAG, "api: socialize.getSDKConfig queued execute");
        new SdkConfigApi(_configuration, getNetworkAdapter(), _sessionManager).call(new GigyaCallback<SdkConfigApi.SdkConfig>() {
            @Override
            public void onSuccess(SdkConfigApi.SdkConfig obj) {
                if (isValidConfiguration()) {
                    _configuration.setIDs(obj.getIds());
                    _configuration.setAppIds(obj.getAppIds());
                    if (_sessionManager != null) {
                        // Trigger session save in order to keep track of GMID, UCID.
                        _sessionManager.save();
                    }
                    _networkAdapter.release();
                }
                if (completionHandler != null) {
                    completionHandler.run();
                }
            }

            @Override
            public void onError(GigyaError error) {
                GigyaLogger.error(LOG_TAG, "getSDKConfig error: " + error.toString());
            }
        });
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
        if (!isValidConfiguration()) {
            GigyaLogger.error(LOG_TAG, "Configuration invalid. Api-Key unavailable");
            return;
        }
        new AnonymousApi<GigyaResponse>(_configuration, getNetworkAdapter(), _sessionManager)
                .call(api, params, callback);
    }

    @NonNull
    private Class<T> _accountClazz = (Class<T>) GigyaAccount.class;

    //endregion

    //region GigyaAccount & Session

    @Nullable
    private LoginProvider _currentProvider;

    @Nullable
    public LoginProvider getLoginProvider() {
        return _currentProvider;
    }

    @Nullable
    private SessionManager _sessionManager;

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
        if (!isValidConfiguration()) {
            GigyaLogger.error(LOG_TAG, "Configuration invalid. Api-Key unavailable");
            return;
        }
        new AnonymousApi<>(_configuration, getNetworkAdapter(), _sessionManager, clazz)
                .call(api, params, callback);
    }

    /*
     * Account object reference (cached).
     */
    private T _account;

    private long _accountInvalidationTimestamp = 0L;
    private boolean _accountOverrideCache = false;

    /*
     * Flush account data (nullify).
     */
    private void invalidateAccount() {
        _account = null;
    }

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
        GigyaPresenter.flush();

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
        invalidateAccount();
        final TreeMap<String, Object> params = new TreeMap<>();
        params.put("loginID", username);
        params.put("password", password);
        params.put("include", "profile,data,subscriptions,preferences");
        new LoginApi<>(_configuration, getNetworkAdapter(), _sessionManager, _accountClazz).call(params, callback, new GigyaInterceptionCallback<T>() {
            @Override
            public void intercept(T obj) {
                _account = obj;
            }
        });
    }

    /**
     * Request account info.
     *
     * @param callback Response listener callback.
     */
    public void getAccount(GigyaCallback<T> callback) {
        if (!_accountOverrideCache && _account != null && System.currentTimeMillis() < _accountInvalidationTimestamp) {
            callback.onSuccess(_account);
            return;
        }
        if (!_accountOverrideCache) {
            GigyaLogger.debug("Gigya Account", "Invalidating cached account");
            // Reset invalidation timestamp
            _accountInvalidationTimestamp = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(_configuration.getAccountCacheTime());
        }
        new GetAccountApi<>(_configuration, getNetworkAdapter(), _sessionManager, _accountClazz)
                .call(callback, new GigyaInterceptionCallback<T>() {
                    @Override
                    public void intercept(T obj) {
                        if (!_accountOverrideCache) {
                            _account = obj;
                        }
                    }
                });
    }

    /**
     * Request account info.
     *
     * @param overrideCache Should override the account caching option. When set to true, the SDK will not cache the account object.
     * @param callback      Response listener callback.
     */
    public void getAccount(final boolean overrideCache, GigyaCallback<T> callback) {
        _accountOverrideCache = overrideCache;
        getAccount(callback);
    }

    /**
     * Set account info
     *
     * @param account  Updated account object.
     * @param callback Response listener callback.
     */
    public void setAccount(T account, GigyaCallback callback) {
        new SetAccountApi<>(_configuration, getNetworkAdapter(), _sessionManager, _accountClazz, account, _account)
                .call(callback, new GigyaInterceptionCallback<T>() {
                    @Override
                    public void intercept(T obj) {
                        // Flush account cache.
                        _account = obj;
                    }
                });
    }

    /**
     * Register account using email & password combination.
     *
     * @param email    User email identifier.
     * @param password User password.
     * @param callback Response listener callback.
     */
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
        invalidateAccount();
        new RegisterApi<>(_configuration, getNetworkAdapter(), _sessionManager, _accountClazz, policy, finalize)
                .call(params, callback, new GigyaInterceptionCallback<T>() {
                    @Override
                    public void intercept(T obj) {
                        // TODO: 18/12/2018 Should we call getAccountInfo here?
                    }
                });
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
                            invalidateAccount();
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
    public void presetNativeLogin(final Map<String, Object> params, final GigyaCallback<T> callback) {
        GigyaPresenter.presentNativeLogin(_appContext, _configuration, params, new LoginProvider.LoginProviderCallbacks() {

            @Override
            public void onConfigurationRequired(final Activity activity, final LoginProvider loginProvider) {
                getSdkConfig(new Runnable() {
                    @Override
                    public void run() {
                        /* Okay to release activity. */
                        activity.finish();

                        if (!_configuration.getAppIds().isEmpty()) {
                            /* Update provider client id if available */
                            final String providerClientId = _configuration.getAppIds().get(loginProvider.getName());
                            if (providerClientId != null) {
                                loginProvider.updateProviderClientId(providerClientId);
                            }
                        }

                        loginProvider.login(_appContext, params);
                    }
                });
            }

            @Override
            public void onProviderSelected(LoginProvider provider) {
                /* Update current provider. */
                _currentProvider = provider;
            }

            @Override
            public void onProviderLoginSuccess(final String provider, String providerSessions) {
                GigyaLogger.debug(LOG_TAG, "onProviderLoginSuccess: provider = "
                        + provider + ", providerSessions = " + providerSessions);

                /* Call intermediate load to give the client the option to trigger his own progress indicator */
                callback.onIntermediateLoad();

                /* Call notifyLogin to complete sign in process.*/
                new NotifyLoginApi<>(_configuration, getNetworkAdapter(), _sessionManager, _accountClazz)
                        .call(providerSessions, callback, new GigyaInterceptionCallback<T>() {
                            @Override
                            public void intercept(T obj) {
                                _account = obj;

                                /* Persist updated provider. */
                                DependencyRegistry.getInstance().getPersistenceHandler(_appContext)
                                        .onLoginProviderUpdated(provider);

                                /* If this provider supports token tracking enable it. */
                                if (_currentProvider != null) {
                                    _currentProvider.trackTokenChanges(_sessionManager);
                                }
                            }
                        });
            }

            @Override
            public void onProviderLoginFailed(String provider, String error) {
                GigyaLogger.debug(LOG_TAG, "onProviderLoginFailed: provider = "
                        + provider + ", error =" + error);

                // TODO: 09/01/2019 Need to provide a detailed error here.
                callback.onError(GigyaError.errorFrom(error));
            }

        }, _loginTrackerCallback);
    }

    //endregion

}
