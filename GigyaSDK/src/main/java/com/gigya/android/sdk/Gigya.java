package com.gigya.android.sdk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import com.gigya.android.sdk.model.account.GigyaAccount;
import com.gigya.android.sdk.model.account.SessionInfo;
import com.gigya.android.sdk.network.GigyaApiResponse;
import com.gigya.android.sdk.providers.LoginProvider;
import com.gigya.android.sdk.services.AccountService;
import com.gigya.android.sdk.services.Config;
import com.gigya.android.sdk.ui.plugin.GigyaPluginPresenter;
import com.gigya.android.sdk.ui.plugin.PluginFragment;
import com.gigya.android.sdk.ui.provider.GigyaLoginPresenter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Gigya<T extends GigyaAccount> {

    private static final String LOG_TAG = "Gigya";

    public static final String VERSION = "android_4.0.0";

    @SuppressLint("StaticFieldLeak")
    private static Gigya _sharedInstance;

    @NonNull
    final private Context _appContext;

    private final GigyaContext<T> _gigyaContext;

    @NonNull
    public Context getContext() {
        return _appContext;
    }

    private Gigya(@NonNull Context appContext, Class<T> accountScheme) {
        _appContext = appContext;
        _gigyaContext = new GigyaContext<>(appContext);
        _gigyaContext.getAccountService().updateAccountSchene(accountScheme);
        init();
//        if (getCurrentProvider() != null) {
//            /* Track provider token changes if necessary. */
//            getCurrentProvider().trackTokenChanges(getSessionManager());
//        }
    }

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
    Generic account type instance getter.
     */
    @SuppressWarnings("unchecked")
    public static synchronized <V extends GigyaAccount> Gigya<V> getInstance(Context appContext, @NonNull Class<V> accountClazz) {
        if (_sharedInstance == null) {
            _sharedInstance = new Gigya(appContext, accountClazz);
        }
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
        Config config = _gigyaContext.getConfig();
        config.updateWith(apiKey, apiDomain);
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
        Config config = _gigyaContext.getConfig();
        if (config.getApiKey() == null) {
            /* Try to from assets JSON file, */
            config = Config.loadFromJson(_appContext);
            if (config == null) {
                /* Try to load fom manifest meta data. */
                config = Config.loadFromManifest(_appContext);
            }
            if (config != null) {
                // Update with new configuration.
                _gigyaContext.setConfig(config);
            }
        }

        /* Set next account invalidation timestamp if available. */
        if (config != null && config.getAccountCacheTime() != 0) {
            final AccountService<T> accountService = _gigyaContext.getAccountService();
            accountService.setAccountCacheTime(config.getAccountCacheTime());
            accountService.nextAccountInvalidationTimestamp();
        }

        // TODO: 05/03/2019 Fix this. How to we keep track of the enabled providers?
        /* Load last provider if exists. */
//        final String lastProviderName = getPersistenceManager().getString("lastLoginProvider", null);
//        if (lastProviderName != null && getCurrentProvider() == null) {
//            final LoginProvider loginProvider = LoginProviderFactory.providerFor(_appContext, config, lastProviderName, null);
//            getAccountManager().updateLoginProvider(loginProvider);
//            if (loginProvider.clientIdRequired()) {
//                /* Must call sdk config to fetch related client ids for login provider. */
//                loadSDKConfig(new Runnable() {
//                    @Override
//                    public void run() {
//                        if (!getConfiguration().getAppIds().isEmpty() && getCurrentProvider() != null) {
//                            final String providerClientId = getConfiguration().getAppIds().get(lastProviderName);
//                            if (providerClientId != null) {
//                                getCurrentProvider().updateProviderClientId(providerClientId);
//                            }
//                        }
//                    }
//                });
//            }
//        }
    }

    //endregion

    /**
     * Update interruption handling.
     * By default, the Gigya SDK will handle various API interruptions to allow simple resolving of certain common errors.
     * Setting interruptions to FALSE will force the end user to handle his own errors.
     *
     * @param sdkHandles False if manually handling all errors.
     */
    public void handleInterruptions(boolean sdkHandles) {
        _gigyaContext.getConfig().setInterruptionsEnabled(sdkHandles);
    }

    //region Business APis
    /*
   Request SDK configuration. Crucial -> fetches GMID fields needed for all requests.
    */
    private void loadSDKConfig(final Runnable completionHandler) {
        GigyaLogger.debug(LOG_TAG, "api: socialize.getSDKConfig queued execute");
        _gigyaContext.getApiService().loadConfig(completionHandler);
    }

    /**
     * Send request to Gigya servers.
     *
     * @param api      Request method identifier.
     * @param params   Additional parameters.
     * @param callback Response listener callback.
     */
    @SuppressWarnings("unchecked")
    public void send(String api, Map<String, Object> params, GigyaCallback<GigyaApiResponse> callback) {
        _gigyaContext.getApiService().send(api, params, callback);
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
        _gigyaContext.getApiService().send(api, params, clazz, callback);
    }

    //endregion

    //region GigyaAccount & Session

    /**
     * Get current session.
     *
     * @return SessionInfo instance.
     */
    @Nullable
    public SessionInfo getSession() {
        return _gigyaContext.getSessionService().getSession();
    }

    /**
     * Check if we currently have a valid session.
     */
    public boolean isLoggedIn() {
        return _gigyaContext.getSessionService().isValidSession();
    }

    /**
     * Logout of Gigya services.
     * This will clean all session related data persistence.
     */
    public void logout() {
        _gigyaContext.getApiService().logout();
        _gigyaContext.getSessionService().clear(); // Will clear preferences as well.
        GigyaLoginPresenter.flush();

        /* Clearing cached cookies. */
        CookieSyncManager.createInstance(_appContext);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();

        /* Logout from social provider (is available). */
//        if (getCurrentProvider() != null) {
//            getCurrentProvider().logout(_appContext);
//        }
    }

    //endregion

    //region Business Apis

    /**
     * Login with provided id & password.
     *
     * @param loginId  LoginID.
     * @param password Login password.
     * @param callback Response listener callback.
     */
    public void login(String loginId, String password, GigyaLoginCallback<T> callback) {
        final Map<String, Object> params = new TreeMap<>();
        params.put("loginID", loginId);
        params.put("password", password);
        params.put("include", "profile,data,subscriptions,preferences");
        _gigyaContext.getApiService().login(params, callback);
    }

    // TODO: 24/02/2019 Add overload with providers stringDef.

    /**
     * Login given a specific 3rd party provider.
     *
     * @param provider Provider name as described & configured in site console
     * @param callback Login response listener callback.
     */
    public void login(String provider, Map<String, Object> params, GigyaLoginCallback<? extends GigyaAccount> callback) {
        new GigyaLoginPresenter().login(_appContext, provider, params, callback);
    }

    /**
     * Add a social connection to an existing user.
     *
     * @param provider Provider name as described & configured in site console.
     * @param callback Login response listener callback.
     */
    public void addConnection(String provider, GigyaLoginCallback<T> callback) {
        // TODO: 05/02/2019 Add new api flow.
    }

    /**
     * Request account info.
     *
     * @param callback Response listener callback.
     */
    public void getAccount(GigyaCallback<T> callback) {
        _gigyaContext.getApiService().getAccount(callback);
    }

    /**
     * Request account info.
     *
     * @param overrideCache Should override the account caching option. When set to true, the SDK will not cache the account object.
     * @param callback      Response listener callback.
     */
    @SuppressWarnings("unused")
    public void getAccount(final boolean overrideCache, GigyaCallback<T> callback) {
        _gigyaContext.getAccountService().setAccountOverrideCache(overrideCache);
        getAccount(callback);
    }

    /**
     * Set account info
     *
     * @param account  Updated account object.
     * @param callback Response listener callback.
     */
    public void setAccount(T account, GigyaCallback<T> callback) {
        _gigyaContext.getApiService().setAccount(account, callback);
    }

    /**
     * Register account using email & password combination.
     *
     * @param email    User email identifier.
     * @param password User password.
     * @param callback Response listener callback.
     */
    @SuppressWarnings("unused")
    public void register(String email, String password, GigyaLoginCallback<T> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("loginID", email);
        params.put("password", password);
        _gigyaContext.getApiService().register(params, callback);
    }

    /**
     * Send a reset email password to verified email attached to the users loginId.
     *
     * @param loginId  User login id.
     * @param callback Response listener callback.
     */
    public void forgotPassword(String loginId, GigyaCallback<GigyaApiResponse> callback) {
        _gigyaContext.getApiService().forgotPassword(loginId, callback);
    }

    //endregion

    //region Native login

    /**
     * Present native login selection according to requested parameters.
     *
     * @param params   Requested parameters.
     * @param callback Response listener callback.
     */
    public void socialLoginWith(@LoginProvider.SocialProvider List<String> providers,
                                final Map<String, Object> params, final GigyaLoginCallback<? extends GigyaAccount> callback) {
        GigyaLogger.debug(LOG_TAG, "socialLoginWith: with parameters:\n" + params.toString());
        new GigyaLoginPresenter().showNativeLoginProviders(_appContext, providers, params, callback);
    }

    //endregion

    //region Plugins

    // TODO: 16/02/2019 JavaDoc
    public <H> void showScreenSets(Map<String, Object> params, final GigyaPluginCallback<H> callback) {
        GigyaLogger.debug(LOG_TAG, "showPlugin: " + PluginFragment.PLUGIN_SCREENSETS + ", with parameters:\n" + params.toString());
        new GigyaPluginPresenter()
                .showPlugin(_appContext, false, PluginFragment.PLUGIN_SCREENSETS, params, callback);
    }

    // TODO: 16/02/2019 JavaDoc
    public <H> void showComments(Map<String, Object> params, final GigyaPluginCallback<H> callback) {
        GigyaLogger.debug(LOG_TAG, "showPlugin: " + PluginFragment.PLUGIN_COMMENTS + ", with parameters:\n" + params.toString());
        new GigyaPluginPresenter()
                .showPlugin(_appContext, false, PluginFragment.PLUGIN_COMMENTS, params, callback);
    }

    //endregion
}
