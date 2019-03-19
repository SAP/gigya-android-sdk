package com.gigya.android.sdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import com.gigya.android.sdk.model.account.GigyaAccount;
import com.gigya.android.sdk.model.account.SessionInfo;
import com.gigya.android.sdk.network.GigyaApiResponse;
import com.gigya.android.sdk.providers.LoginProvider;
import com.gigya.android.sdk.providers.LoginProviderFactory;
import com.gigya.android.sdk.services.AccountService;
import com.gigya.android.sdk.services.Config;
import com.gigya.android.sdk.ui.plugin.GigyaPluginPresenter;
import com.gigya.android.sdk.ui.plugin.PluginFragment;
import com.gigya.android.sdk.ui.provider.GigyaLoginPresenter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class Gigya<T extends GigyaAccount> {

    private static final String LOG_TAG = "Gigya";

    public static final String VERSION = "android_4.0.0_a2";

    @SuppressLint("StaticFieldLeak")
    private static Gigya _sharedInstance;

    @NonNull
    final private Context _appContext;

    private final GigyaContext<T> _gigyaContext;

    private ArrayMap<String, LoginProvider> _usedLoginProviders = new ArrayMap<>();

    @NonNull
    public Context getContext() {
        return _appContext;
    }

    private Gigya(@NonNull Context appContext, Class<T> accountScheme) {
        _appContext = appContext;
        _gigyaContext = new GigyaContext<>(appContext);
        _gigyaContext.getAccountService().updateAccountScheme(accountScheme);
        init();
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
            _sharedInstance.registerActivityLifecycleCallbacks();
        }
        final Class scheme = _sharedInstance._gigyaContext.getAccountService().getAccountScheme();
        if (scheme != accountClazz) {
            GigyaLogger.error(LOG_TAG, "Scheme already set in previous initialization.\nSDK does not allow to override a set scheme.");
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
            // Try to from assets JSON file,
            config = Config.loadFromJson(_appContext);
            if (config == null) {
                // Try to load fom manifest meta data.
                config = Config.loadFromManifest(_appContext);
            }
            if (config != null) {
                // Update with new configuration.
                _gigyaContext.setConfig(config);
            }
        }

        // Set next account invalidation timestamp if available.
        if (config != null && config.getAccountCacheTime() != 0) {
            final AccountService<T> accountService = _gigyaContext.getAccountService();
            accountService.setAccountCacheTime(config.getAccountCacheTime());
            accountService.nextAccountInvalidationTimestamp();
        }

        // Check if any social providers are used. If so. Instantiate them and check for server available client ids.
        final Set<String> usedSocialProviders = _gigyaContext.getPersistenceService().getSocialProviders();
        if (usedSocialProviders != null) {
            for (String identifier : usedSocialProviders) {
                final LoginProvider provider = LoginProviderFactory.providerFor(_appContext, _gigyaContext.getApiService(), identifier, null);
                _usedLoginProviders.put(identifier, provider);

                // TODO: 19/03/2019 Consider removing this part. Not really aligned with SDK design pattern.

                if (provider.clientIdRequired()) {
                    // Must call sdk config to fetch related client ids for login provider.
                    loadSDKConfig(new Runnable() {
                        @Override
                        public void run() {
                            final Map<String, String> appIds = _gigyaContext.getConfig().getAppIds();
                            if (appIds.containsKey(provider.getName())) {
                                final String providerClientId = appIds.get(provider.getName());
                                provider.updateProviderClientId(providerClientId);
                            }
                        }
                    });
                }
            }
        }
    }

    //region Lifecycle callbacks

    /**
     * Attaching the SDK to the application lifecycle in order to distinguish foreground/background/resumed states.
     */
    private void registerActivityLifecycleCallbacks() {
        if (!(_appContext instanceof Application)) {
            GigyaLogger.error(LOG_TAG, "SDK initialized with the wrong context. Please make sure you have initialized the SDK using the applicationContext");
            return;
        }
        ((Application) _appContext).registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {

            private int activityReferences = 0;
            private boolean isActivityChangingConfigurations = false;

            @Override
            public void onActivityCreated(Activity activity, Bundle bundle) {
                // Stub.
            }

            @Override
            public void onActivityStarted(Activity activity) {
                if (++activityReferences == 1 && !isActivityChangingConfigurations) {
                    // App enters foreground
                    GigyaLogger.info(LOG_TAG, "Application lifecycle - Foreground");
                    // Will start session countdown timer if the current session contains an expiration time.
                    _gigyaContext.getSessionService().startSessionCountdownTimerIfNeeded();
                    if (isLoggedIn()) {
                        // Session verification is only relevant when user is logged in.
                        _gigyaContext.getSessionVerificationService().start();
                    }
                }
            }

            @Override
            public void onActivityResumed(Activity activity) {
                // Stub. Can track the current resumed activity.
            }

            @Override
            public void onActivityPaused(Activity activity) {
                // Stub.
            }

            @Override
            public void onActivityStopped(Activity activity) {
                isActivityChangingConfigurations = activity.isChangingConfigurations();
                if (--activityReferences == 0 && !isActivityChangingConfigurations) {
                    // App enters background
                    GigyaLogger.info(LOG_TAG, "Application lifecycle - Background");
                    // Make sure to cancel the session expiration countdown timer (if live).
                    _gigyaContext.getSessionService().cancelSessionCountdownTimer();
                    _gigyaContext.getSessionVerificationService().stop();
                }
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
                // Stub.
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                // Stub.
            }
        });
    }

    //endregion

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

    /**
     * Get reference to used social login provider (if exists) given an identifier.
     *
     * @param provider Provider name identifier {@link GigyaDefinitions.Providers.SocialProvider}.
     * @return LoginProvider instance.
     */
    @Nullable
    public LoginProvider getSocialProvider(@GigyaDefinitions.Providers.SocialProvider String provider) {
        if (_usedLoginProviders.containsKey(provider)) {
            return _usedLoginProviders.get(provider);
        }
        return null;
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
        GigyaLogger.debug(LOG_TAG, "logout: ");
        _gigyaContext.getApiService().logout();
        _gigyaContext.getSessionService().clear(); // Will clear preferences as well.
        GigyaLoginPresenter.flush();

        // Clearing cached cookies.
        CookieSyncManager.createInstance(_appContext);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();

        // Logout of any available social provider.
        for (Map.Entry<String, LoginProvider> entry : _usedLoginProviders.entrySet()) {
            entry.getValue().logout(_appContext);
        }
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
        GigyaLogger.debug(LOG_TAG, "login: with loginId = " + loginId);
        final Map<String, Object> params = new TreeMap<>();
        params.put("loginID", loginId);
        params.put("password", password);
        params.put("include", "profile,data,subscriptions,preferences");
        _gigyaContext.getApiService().login(params, callback);
    }

    /**
     * Login with given parameters.
     *
     * @param params   parameters map.
     * @param callback gin response callback.
     */
    public void login(Map<String, Object> params, GigyaLoginCallback<T> callback) {
        GigyaLogger.debug(LOG_TAG, "login: with params = " + params.toString());
        params.put("include", "profile,data,subscriptions,preferences");
        _gigyaContext.getApiService().login(params, callback);
    }

    /**
     * Login given a specific 3rd party provider.
     *
     * @param socialProvider Selected providers {@link GigyaDefinitions.Providers.SocialProvider}.
     * @param params         Parameters map.
     * @param callback       Login response callback.
     */
    public void login(@GigyaDefinitions.Providers.SocialProvider String socialProvider, Map<String, Object> params, GigyaLoginCallback<T> callback) {
        GigyaLogger.debug(LOG_TAG, "login: with provider = " + socialProvider);
        new GigyaLoginPresenter(_gigyaContext).login(_appContext, socialProvider, params, callback);
    }

    /**
     * Request account info.
     *
     * @param callback Response listener callback.
     */
    public void getAccount(GigyaCallback<T> callback) {
        GigyaLogger.debug(LOG_TAG, "getAccount: ");
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
        GigyaLogger.debug(LOG_TAG, "getAccount: overrideCache = " + overrideCache);
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
        GigyaLogger.debug(LOG_TAG, "setAccount: ");
        _gigyaContext.getApiService().setAccount(account, callback);
    }

    /**
     * Request verify login given account UID/
     *
     * @param UID      Account UID identifier.
     * @param callback Response listener callback.
     */
    public void verifyLogin(String UID, GigyaCallback<T> callback) {
        GigyaLogger.debug(LOG_TAG, "verifyLogin: for UID = " + UID);
        _gigyaContext.getApiService().verifyLogin(UID, false,  callback);
    }

    /**
     * Register account using email & password combination.
     * Additional parameters are included to allow additional parameters to be added such as profile.
     *
     * @param email    User email identifier.
     * @param password User password.
     * @param params   Additional parameters.
     * @param callback Response listener callback.
     */
    public void register(String email, String password, Map<String, Object> params, GigyaLoginCallback<T> callback) {
        GigyaLogger.debug(LOG_TAG, "register: with email: " + email + " and params: " + params.toString());
        params.put("email", email);
        params.put("password", password);
        _gigyaContext.getApiService().register(params, callback);
    }

    /**
     * Register account using email & password combination.
     *
     * @param email    User email identifier.
     * @param password User password.
     * @param callback Response listener callback.
     */
    public void register(String email, String password, GigyaLoginCallback<T> callback) {
        GigyaLogger.debug(LOG_TAG, "register: with email: " + email);
        Map<String, Object> params = new HashMap<>();
        register(email, password, params, callback);
    }

    /**
     * Send a reset email password to verified email attached to the users loginId.
     *
     * @param loginId  User login id.
     * @param callback Response listener callback.
     */
    public void forgotPassword(String loginId, GigyaCallback<GigyaApiResponse> callback) {
        GigyaLogger.debug(LOG_TAG, "forgotPassword: with " + loginId);
        _gigyaContext.getApiService().forgotPassword(loginId, callback);
    }

    //endregion

    //region Native login

    /**
     * Present social login selection list.
     *
     * @param providers List of selected social providers {@link GigyaDefinitions.Providers.SocialProvider}.
     * @param params    Request parameters.
     * @param callback  Login response callback.
     */
    public void socialLoginWith(@GigyaDefinitions.Providers.SocialProvider List<String> providers,
                                final Map<String, Object> params, final GigyaLoginCallback<T> callback) {
        GigyaLogger.debug(LOG_TAG, "socialLoginWith: with parameters:\n" + params.toString());
        new GigyaLoginPresenter(_gigyaContext).showNativeLoginProviders(_appContext, providers, params, callback);
    }

    //endregion

    //region Plugins

    /**
     * Show Gigya ScreenSets flow using the PluginFragment.
     * UI will be presented via WebView.
     *
     * @param screensSet Main ScreensSet group identifier
     * @param params     ScreensSet flow parameters.
     * @param callback   Plugin callback.
     */
    public void showScreenSets(final String screensSet, final Map<String, Object> params, final GigyaPluginCallback<T> callback) {
        params.put("screenSet", screensSet);
        GigyaLogger.debug(LOG_TAG, "showPlugin: " + PluginFragment.PLUGIN_SCREENSETS + ", with parameters:\n" + params.toString());
        new GigyaPluginPresenter(_gigyaContext)
                .showPlugin(_appContext, false, PluginFragment.PLUGIN_SCREENSETS, params, callback);

    }

    /**
     * Show Comments ScreenSets.
     *
     * @param params   Comments ScreenSet flow parameters.
     * @param callback Plugin callback.
     */
    public void showComments(Map<String, Object> params, final GigyaPluginCallback<T> callback) {
        GigyaLogger.debug(LOG_TAG, "showPlugin: " + PluginFragment.PLUGIN_COMMENTS + ", with parameters:\n" + params.toString());
        new GigyaPluginPresenter(_gigyaContext)
                .showPlugin(_appContext, false, PluginFragment.PLUGIN_COMMENTS, params, callback);
    }

    //endregion
}
