package com.gigya.android.sdk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.log.GigyaLogger;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.model.GigyaAccount;
import com.gigya.android.sdk.model.SessionInfo;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.GigyaInterceptionCallback;
import com.gigya.android.sdk.network.GigyaRequestOld;
import com.gigya.android.sdk.network.GigyaRequestQueue;
import com.gigya.android.sdk.network.GigyaResponse;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;
import com.gigya.android.sdk.network.api.AnonymousApi;
import com.gigya.android.sdk.network.api.GetAccountApi;
import com.gigya.android.sdk.network.api.LoginApi;
import com.gigya.android.sdk.network.api.LogoutApi;
import com.gigya.android.sdk.network.api.RegisterApi;
import com.gigya.android.sdk.network.api.SdkConfigApi;
import com.gigya.android.sdk.network.api.SetAccountApi;
import com.gigya.android.sdk.ui.GigyaPresenter;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

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
        this._sessionManager = new SessionManager(this);
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
    }

    //endregion

    //region Networking

    /**
     * Implicitly initialize the SDK.
     * Available Options:
     * - read JSON assets file.
     * - parse application manifest meta data tags.
     * For explicit setting see {@link #init(String, String)} method.
     */
    private void init() {
        /* Try to from assets JSON file, */
        _configuration = Configuration.loadFromJson(_appContext);
        if (_configuration == null) {
            /* Try to load fom manifest meta data. */
            _configuration = Configuration.loadFromManifest(_appContext);
        }
    }

    private NetworkAdapter getNetworkAdapter() {
        if (_networkAdapter == null) {
            _networkAdapter = new NetworkAdapter(_appContext);
        }
        return _networkAdapter;
    }

    /*
    Main network priority request queue.
     */
    private GigyaRequestQueue _requestQueue;

    private GigyaRequestQueue getRequestQueue() {
        if (_requestQueue == null) {
            _requestQueue = new GigyaRequestQueue(_appContext);
        }
        return _requestQueue;
    }

    /*
    Actual SDK send request method.
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    private void send(GigyaRequestOld request) {
        if (!_configuration.hasGMID()) {
            getSdkConfig();
            _requestQueue.block();
        }
        getRequestQueue().add(request);
    }

    /*
   Request SDK configuration. Crucial -> fetches GMID fields needed for all requests.
    */
    private void getSdkConfig() {
        GigyaLogger.debug(LOG_TAG, "api: socialize.getSDKConfig queued execute");
        new SdkConfigApi(_configuration, _networkAdapter, _sessionManager).call(new GigyaCallback<SdkConfigApi.SdkConfig>() {
            @Override
            public void onSuccess(SdkConfigApi.SdkConfig obj) {
                if (isValidConfiguration()) {
                    _configuration.setIDs(obj.getIds());
                    if (_sessionManager != null) {
                        // Trigger session save in order to keep track of GMID, UCID.
                        _sessionManager.save();
                    }
                    _networkAdapter.release();
                }
            }

            @Override
            public void onError(GigyaError error) {
                GigyaLogger.error(LOG_TAG, "getSDKConfig error: " + error.toString());
            }
        });

//        getRequestQueue().add(new SdkConfigApi(_configuration, _sessionManager).getRequest(null, new GigyaCallback<SdkConfigApi.SdkConfig>() {
//            @Override
//            public void onSuccess(SdkConfigApi.SdkConfig obj) {
//                if (isValidConfiguration()) {
//                    _configuration.setIDs(obj.getIds());
//                    if (_sessionManager != null) {
//                        // Trigger session save in order to keep track of GMID, UCID.
//                        _sessionManager.save();
//                    }
//                    _requestQueue.release();
//                }
//            }
//
//            @Override
//            public void onError(GigyaError error) {
//                GigyaLogger.error(LOG_TAG, "getSDKConfig error: " + error.toString());
//            }
//        }, null));
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
//        send(new GigyaRequestBuilderOld(_configuration)
//                .api(api)
//                .sessionManager(_sessionManager)
//                .params(params)
//                .callback(callback)
//                .build());
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
        if (!isValidConfiguration()) {
            GigyaLogger.error(LOG_TAG, "Configuration invalid. Api-Key unavailable");
            return;
        }
        new AnonymousApi<>(_configuration, getNetworkAdapter(), _sessionManager,  clazz)
                .call(api, params, callback);
//        send(new GigyaRequestBuilderOld(_configuration)
//                .api(api)
//                .sessionManager(_sessionManager)
//                .params(params)
//                .output(clazz)
//                .callback(callback)
//                .build());
    }

    //endregion

    //region BaseGigyaAccount & Session

    @Nullable
    private SessionManager _sessionManager;

    @Nullable
    private Class<T> _accountClazz = (Class<T>) GigyaAccount.class;

    /*
     * Account object reference (cached).
     */
    private T _account;

    /*
     * Flush account data (nullify).
     */
    private void invalidateAccount() {
        _account = null;
    }

    // TODO: 01/01/2019 This is a testing only method.
    /**
     * Manually set the current Account scheme.
     * Use this option if applying a custom account scheme and did not initialized the SDK using a
     * custom account class reference.
     *
     * @param accountClazz Custom account reference.
     */
    public void setAccountScheme(@NonNull Class<T> accountClazz) {
        invalidateAccount();
        _accountClazz = accountClazz;
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
//        send("socialize.logout", null, null);
        new LogoutApi(_configuration, getNetworkAdapter(), _sessionManager).call();
        if (_sessionManager != null) {
            _sessionManager.clear();
        }
        getNetworkAdapter().cancel(null);
        // TODO: 05/12/2018 Additional handling required on provider logic implementation.
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
//        send(new LoginApi<>(_configuration, _sessionManager, _accountClazz).getRequest(params, callback, new GigyaInterceptionCallback<T>() {
//            @Override
//            public void intercept(T obj) {
//                // TODO: 20/12/2018 Interception here might be a problem. Should we call getAccountInfo in login flow?
//                _account = obj;
//            }
//        }));
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
        // TODO: 06/12/2018 BaseGigyaAccount caching policy.
//        if (_account != null) {
//            // Cached BaseGigyaAccount instance. Return it.
//            callback.onSuccess(_account);
//            return;
//        }
        new GetAccountApi<>(_configuration, getNetworkAdapter(), _sessionManager, _accountClazz)
                .call(callback, new GigyaInterceptionCallback<T>() {
            @Override
            public void intercept(T obj) {
                _account = obj;
            }
        });
//        send(new GetAccountApi<>(_configuration, _sessionManager, _accountClazz).getRequest(null, callback, new GigyaInterceptionCallback<T>() {
//
//            @Override
//            public void intercept(T obj) {
//                _account = obj;
//            }
//        }));
    }

    /**
     * Set account info
     *
     * @param account  Updated account object.
     * @param callback Response listener callback.
     */
    public void setAccount(T account, GigyaCallback callback) {
//        send(new SetAccountApi<>(_configuration, _sessionManager, account, _account)
//                .getRequest(null, callback, new GigyaInterceptionCallback() {
//                    @Override
//                    public void intercept(Object obj) {
//                        // Flush account cache.
//                        _account = null;
//                        // TODO: 17/12/2018 Maybe we should make another call to getAccount here?
//                    }
//                }));
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

    private void register(Map<String, Object> params, RegisterApi.RegisterPolicy policy, boolean finalize, GigyaRegisterCallback<T> callback) {
        invalidateAccount();
        new RegisterApi<>(_configuration, getNetworkAdapter(), _sessionManager, _accountClazz, policy, finalize)
                .call(params, callback, new GigyaInterceptionCallback<T>() {
                    @Override
                    public void intercept(T obj) {
                        // TODO: 18/12/2018 Should we call getAccountInfo here?
                    }
                });
//        send(new RegisterApi<>(_configuration, _sessionManager, _requestQueue, _accountClazz, policy, finalize)
//                .getRequest(params, callback, new GigyaInterceptionCallback<T>() {
//                    @Override
//                    public void intercept(T obj) {
//                       // Stub.
//                        // TODO: 18/12/2018 Should we call getAccountInfo here?
//                    }
//                })
//        );
    }

    //endregion

    //region User Interface & presentation

    public void presetNativeLogin(Map<String, Object> params) {
        GigyaPresenter.presentNativeLogin(_appContext, _configuration, params);
    }

    //endregion

}
