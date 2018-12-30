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
import com.gigya.android.sdk.network.GigyaRequest;
import com.gigya.android.sdk.network.GigyaRequestBuilder;
import com.gigya.android.sdk.network.GigyaRequestQueue;
import com.gigya.android.sdk.network.GigyaResponse;
import com.gigya.android.sdk.network.api.GetAccountApi;
import com.gigya.android.sdk.network.api.LoginApi;
import com.gigya.android.sdk.network.api.RegisterApi;
import com.gigya.android.sdk.network.api.SdkConfigApi;
import com.gigya.android.sdk.network.api.SetAccountApi;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class Gigya<T> {

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

    /*
    Simplified instance getter for use only after calling getInstance(Context context) at least once.
     */
    // TODO: 10/12/2018 Error logs will need pronunciation review from product
    public static synchronized Gigya getInstance() {
        if (_sharedInstance == null) {
            // Log error.
            GigyaLogger.error(LOG_TAG, "Gigya instance not initialized properly!" +
                    " Make sure to call Gigya getInstance(Context appContext) at least once before trying to reference The Gigya instance");
            return null;
        }
        return _sharedInstance;
    }

    /*
    Simplified instance getter.
     */
    public static synchronized Gigya getInstance(Context appContext) {
        if (_sharedInstance == null) {
            _sharedInstance = new Gigya(appContext);
        }
        return _sharedInstance;
    }

    /*
    Generic account type instance getter.
     */
    public static synchronized <T> Gigya getInstance(Context appContext, @NonNull Class<T> accountClazz) {
        if (_sharedInstance == null) {
            _sharedInstance = new Gigya(appContext);
        }
        _sharedInstance._accountClazz = accountClazz;
        return _sharedInstance;
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

    /**
     * Implicitly initialize the SDK.
     * Available Options:
     * - read JSON assets file.
     * - parse application manifest meta data tags.
     * For explicit setting see {@link #init(String, String)} method.
     */
    private void init() {
        // Try to from assets JSON file,
        _configuration = Configuration.loadFromJson(_appContext);
        if (_configuration == null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB_MR1) {
            // Try to load fom manifest meta data.
            _configuration = Configuration.loadFromManifest(_appContext);
        }
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
    private void send(GigyaRequest request) {
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
        getRequestQueue().add(new SdkConfigApi(_configuration, _sessionManager).getRequest(null, new GigyaCallback<SdkConfigApi.SdkConfig>() {
            @Override
            public void onSuccess(SdkConfigApi.SdkConfig obj) {
                if (isValidConfiguration()) {
                    _configuration.setIDs(obj.getIds());
                    if (_sessionManager != null) {
                        // Trigger session save in order to keep track of GMID, UCID.
                        _sessionManager.save();
                    }
                    _requestQueue.release();
                }
            }

            @Override
            public void onError(GigyaError error) {
                GigyaLogger.error(LOG_TAG, "getSDKConfig error: " + error.toString());
            }
        }, null));
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
        send(new GigyaRequestBuilder(_configuration)
                .api(api)
                .sessionManager(_sessionManager)
                .params(params)
                .callback(callback)
                .build());
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
    public <V> void send(String api, Map<String, Object> params, Class<V> clazz, GigyaCallback<V> callback) {
        if (!isValidConfiguration()) {
            GigyaLogger.error(LOG_TAG, "Configuration invalid. Api-Key unavailable");
            return;
        }
        send(new GigyaRequestBuilder(_configuration)
                .api(api)
                .sessionManager(_sessionManager)
                .params(params)
                .output(clazz)
                .callback(callback)
                .build());
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
        send("socialize.logout", null, null);
        if (_sessionManager != null) {
            _sessionManager.clear();
        }
        _requestQueue.cancelAll();
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
        send(new LoginApi<>(_configuration, _sessionManager, _accountClazz).getRequest(params, callback, new GigyaInterceptionCallback<T>() {
            @Override
            public void intercept(T obj) {
                // TODO: 20/12/2018 Interception here might be a problem. Should we call getAccountInfo in login flow?
                _account = obj;
            }
        }));
    }

    /**
     * Request account info.
     *
     * @param callback Response listener callback.
     */
    public void getAccount(GigyaCallback<T> callback) {
        // TODO: 06/12/2018 BaseGigyaAccount caching policy.
        if (_account != null) {
            // Cached BaseGigyaAccount instance. Return it.
            callback.onSuccess(_account);
            return;
        }
        send(new GetAccountApi<>(_configuration, _sessionManager, _accountClazz).getRequest(null, callback, new GigyaInterceptionCallback<T>() {

            @Override
            public void intercept(T obj) {
                _account = obj;
            }
        }));
    }

    /**
     * Set account info
     *
     * @param account  Updated account object.
     * @param callback Response listener callback.
     */
    public void setAccount(T account, GigyaCallback callback) {
        send(new SetAccountApi<>(_configuration, _sessionManager, account, _account)
                .getRequest(null, callback, new GigyaInterceptionCallback() {
                    @Override
                    public void intercept(Object obj) {
                        // Flush account cache.
                        _account = null;
                        // TODO: 17/12/2018 Maybe we should make another call to getAccount here?
                    }
                }));
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
        send(new RegisterApi<>(_configuration, _sessionManager, _requestQueue, _accountClazz, policy, finalize)
                .getRequest(params, callback, new GigyaInterceptionCallback<T>() {
                    @Override
                    public void intercept(T obj) {
                       // Stub.
                        // TODO: 18/12/2018 Should we call getAccountInfo here?
                    }
                })
        );
    }

    //endregion

}
