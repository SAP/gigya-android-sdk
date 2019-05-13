package com.gigya.android.sdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import com.gigya.android.sdk.account.IAccountService;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.containers.GigyaContainer;
import com.gigya.android.sdk.containers.IoCContainer;
import com.gigya.android.sdk.interruption.IInterruptionsHandler;
import com.gigya.android.sdk.model.account.GigyaAccount;
import com.gigya.android.sdk.model.account.GigyaAccountClass;
import com.gigya.android.sdk.model.account.SessionInfo;
import com.gigya.android.sdk.network.GigyaApiResponse;
import com.gigya.android.sdk.network.adapter.RestAdapter;
import com.gigya.android.sdk.persistence.IPersistenceService;
import com.gigya.android.sdk.providers.IProviderFactory;
import com.gigya.android.sdk.providers.provider.IProvider;
import com.gigya.android.sdk.session.ISessionService;
import com.gigya.android.sdk.session.ISessionVerificationService;
import com.gigya.android.sdk.ui.IPresenter;
import com.gigya.android.sdk.ui.Presenter;
import com.gigya.android.sdk.ui.plugin.PluginFragment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Gigya SDK main interface.
 * Provides access to the Gigya services.
 *
 * @param <T> Generic account scheme. Extended from base GigyaAccount model.
 */
public class Gigya<T extends GigyaAccount> {

    //region static
    public static final String VERSION = "android_4.0.0_beta_1";

    private static final String LOG_TAG = "Gigya";

    /**
     * Gigya default api domain.
     */
    private static final String DEFAULT_API_DOMAIN = "us1.gigya.com";

    private static IoCContainer CONTAINER;

    public static IoCContainer getContainer() {
        if (CONTAINER == null) {
            CONTAINER = new GigyaContainer();
        }
        return CONTAINER;
    }

    public static void setApplication(Application appContext) {
        getContainer()
                .bind(Application.class, appContext)
                .bind(Context.class, appContext);
    }

    @SuppressLint("StaticFieldLeak")
    private static Gigya INSTANCE;

    /*
    Simplified instance getter for use only after calling getInstance(Context context) at least once.
    */
    @SuppressWarnings("unchecked")
    public static synchronized Gigya<? extends GigyaAccount> getInstance() {
        if (INSTANCE == null) {
            return getInstance(GigyaAccount.class);
        }
        return INSTANCE;
    }

    /*
    Generic account type instance getter.
    */
    @SuppressWarnings("unchecked")
    public static synchronized <V extends GigyaAccount> Gigya<V> getInstance(@NonNull Class<V> accountClazz) {
        if (INSTANCE == null) {
            IoCContainer container = getContainer();
            container.bind(GigyaAccountClass.class, new GigyaAccountClass(accountClazz));

            try {
                INSTANCE = container.createInstance(Gigya.class);
            } catch (Exception e) {
                GigyaLogger.error(LOG_TAG, "Error creating Gigya SDK (did you forget to Gigya.setApplication?)");
                e.printStackTrace();
            }
        }
        // Check scheme. If already set log an error.
        final Class schema = INSTANCE.getAccountSchema();
        if (schema != accountClazz) {
            GigyaLogger.error(LOG_TAG, "Scheme already set in previous initialization.\nSDK does not allow to override a set scheme.");
        }
        return INSTANCE;
    }

    //endregion

    final private Application _context;
    /**
     * SDK main configuration structure.
     */
    final private Config _config;
    final private ConfigFactory _configFactory;
    final private ISessionService _sessionService;
    final private IAccountService<T> _accountService;
    final private IBusinessApiService<T> _businessApiService;
    final private ISessionVerificationService _sessionVerificationService;
    final private IInterruptionsHandler _interruptionsHandler;
    final private IPresenter _presenter;
    final private IProviderFactory _providerFactory;

    @SuppressWarnings("unchecked")
    protected Gigya(@NonNull Application context,
                    Config config,
                    ConfigFactory configFactory,
                    ISessionService sessionService,
                    IAccountService<T> accountService,
                    IBusinessApiService<T> businessApiService,
                    ISessionVerificationService sessionVerificationService,
                    IInterruptionsHandler interruptionsHandler,
                    IPresenter presenter,
                    IProviderFactory providerFactory) {
        // Setup dependencies.
        _context = context;
        _config = config;
        _configFactory = configFactory;
        _sessionService = sessionService;
        _accountService = accountService;
        _businessApiService = businessApiService;
        _sessionVerificationService = sessionVerificationService;
        _interruptionsHandler = interruptionsHandler;
        _presenter = presenter;
        _providerFactory = providerFactory;

        // Setup sdk
        _sessionVerificationService.registerActivityLifecycleCallbacks();
        _sessionService.load();
        init();
    }

    //region INITIALIZE

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
        _config.updateWith(apiKey, apiDomain);
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
        // Will load configuration fields only if none have yet to be set.
        if (_config.getApiKey() == null) {
            // Try to from assets JSON file,
            Config dynamicConfig = _configFactory.load();
            _config.updateWith(dynamicConfig);
        }

        // Set next account invalidation timestamp if available.
        if (_config.getAccountCacheTime() != 0) {
            _accountService.nextAccountInvalidationTimestamp();
        }
    }

    //endregion

    //region PUBLIC INTERFACING

    public Class<T> getAccountSchema() {
        return _accountService.getAccountSchema();
    }

    public Context getContext() {
        return _context;
    }

    /**
     * Update interruption handling.
     * By default, the Gigya SDK will handle various API interruptions to allow simple resolving of certain common errors.
     * Setting interruptions to FALSE will force the end user to handle his own errors.
     *
     * @param sdkHandles False if manually handling all errors.
     */
    public void handleInterruptions(boolean sdkHandles) {
        _interruptionsHandler.setEnabled(sdkHandles);
    }

    /**
     * Return SDK interruptions state.
     * if TRUE, interruption handling will be optional via the GigyaLoginCallback.
     */
    public boolean interruptionsEnabled() {
        return _interruptionsHandler.isEnabled();
    }

    //endregion

    //region ANONYMOUS APIS

    /**
     * Send request to Gigya servers.
     *
     * @param api           Request method identifier.
     * @param params        Additional parameters.
     * @param gigyaCallback Response listener callback.
     */
    @SuppressWarnings("unchecked")
    public void send(String api, Map<String, Object> params, GigyaCallback<GigyaApiResponse> gigyaCallback) {
        _businessApiService.send(api, params, RestAdapter.POST, GigyaApiResponse.class, gigyaCallback);
    }

    /**
     * Send a generic type request to Gigya servers.
     *
     * @param api           Request method identifier.
     * @param params        Additional parameters.
     * @param requestMethod Request method (GET, POST).
     * @param clazz         Response class scheme.
     * @param gigyaCallback Response listener callback.
     */
    @SuppressWarnings("unchecked")
    public <V> void send(String api, Map<String, Object> params, int requestMethod, Class<V> clazz, GigyaCallback<V> gigyaCallback) {
        _businessApiService.send(api, params, requestMethod, clazz, gigyaCallback);
    }

    //endregion

    //region GIGYA ACCOUNT & SESSION

    /**
     * Get current session.
     *
     * @return SessionInfo instance.
     */
    @Nullable
    public SessionInfo getSession() {
        return _sessionService.getSession();
    }

    /**
     * Check if we currently have a valid session.
     */
    public boolean isLoggedIn() {
        return _sessionService.isValid();
    }

    /**
     * Logout of Gigya services.
     * This will clean all session related data persistence.
     */
    @SuppressWarnings("deprecation")
    @SuppressLint("ObsoleteSdkInt")
    public void logout() {
        GigyaLogger.debug(LOG_TAG, "logout: ");
        _businessApiService.logout(null);
        _sessionService.clear(true);

        // TODO: #baryo move to the class responsible for screensets - Presenter?
        // Clearing cached cookies.
        CookieManager cookieManager = CookieManager.getInstance();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.flush();
        } else {
            CookieSyncManager.createInstance(_context);
            cookieManager.removeAllCookie();
        }

        _providerFactory.logoutFromUsedSocialProviders();
    }

    //endregion

    //region BUSINESS APIS

    /**
     * Login with provided id and password.
     *
     * @param loginId       LoginID.
     * @param password      Login password.
     * @param gigyaCallback Response listener callback.
     */
    public void login(String loginId, String password, GigyaLoginCallback<T> gigyaCallback) {
        GigyaLogger.debug(LOG_TAG, "login: with loginId = " + loginId);
        final Map<String, Object> params = new TreeMap<>();
        params.put("loginID", loginId);
        params.put("password", password);
        params.put("include", "profile,data,subscriptions,preferences");
        login(params, gigyaCallback);
    }

    /**
     * Login with given parameters.
     *
     * @param params             parameters map.
     * @param gigyaLoginCallback Login response callback.
     */
    public void login(Map<String, Object> params, final GigyaLoginCallback<T> gigyaLoginCallback) {
        GigyaLogger.debug(LOG_TAG, "login: with params = " + params.toString());
        params.put("include", "profile,data,subscriptions,preferences");
        _businessApiService.login(params, gigyaLoginCallback);
    }

    /**
     * Login given a specific 3rd party provider.
     *
     * @param socialProvider     Selected providers {@link GigyaDefinitions.Providers.SocialProvider}.
     * @param params             Parameters map.
     * @param gigyaLoginCallback Login response callback.
     */
    public void login(@GigyaDefinitions.Providers.SocialProvider String socialProvider, Map<String, Object> params, GigyaLoginCallback<T> gigyaLoginCallback) {
        GigyaLogger.debug(LOG_TAG, "login: with provider = " + socialProvider);
        _businessApiService.login(socialProvider, params, gigyaLoginCallback);
    }

    /**
     * Request account info.
     *
     * @param gigyaCallback Response listener callback.
     */
    public void getAccount(GigyaCallback<T> gigyaCallback) {
        GigyaLogger.debug(LOG_TAG, "getAccount: ");
        getAccount(false, gigyaCallback);
    }

    /**
     * Request account info.
     *
     * @param overrideCache Should override the account caching option. When set to true, the SDK will not cache the account object.
     * @param gigyaCallback Response listener callback.
     */
    @SuppressWarnings("unused")
    public void getAccount(final boolean overrideCache, GigyaCallback<T> gigyaCallback) {
        GigyaLogger.debug(LOG_TAG, "getAccount: overrideCache = " + overrideCache);
        if (overrideCache) {
            _accountService.setAccountOverrideCache(overrideCache);
        }
        _businessApiService.getAccount(gigyaCallback);
    }

    /**
     * Set account info
     *
     * @param account       Updated account object.
     * @param gigyaCallback Response listener callback.
     */
    public void setAccount(T account, GigyaCallback<T> gigyaCallback) {
        GigyaLogger.debug(LOG_TAG, "setAccount: ");
        _businessApiService.setAccount(account, gigyaCallback);
    }

    /**
     * Set account info given update parameters.
     *
     * @param params        Updated account parameters.
     * @param gigyaCallback Response listener callback.
     */
    public void setAccount(Map<String, Object> params, GigyaCallback<T> gigyaCallback) {
        GigyaLogger.debug(LOG_TAG, "setAccount: with params");
        _businessApiService.setAccount(params, gigyaCallback);
    }

    /**
     * Request verify login given account UID/
     *
     * @param UID           Account UID identifier.
     * @param gigyaCallback Response listener callback.
     */
    public void verifyLogin(String UID, GigyaCallback<T> gigyaCallback) {
        GigyaLogger.debug(LOG_TAG, "verifyLogin: for UID = " + UID);
        _businessApiService.verifyLogin(UID, gigyaCallback);
    }

    /**
     * Register account using email and password combination.
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
        _businessApiService.register(params, callback);
    }

    /**
     * Register account using email and password combination.
     *
     * @param email    User email identifier.
     * @param password User password.
     * @param callback Response listener callback.
     */
    public void register(String email, String password, GigyaLoginCallback<T> callback) {
        GigyaLogger.debug(LOG_TAG, "register: with email: " + email);
        final Map<String, Object> params = new HashMap<>();
        register(email, password, params, callback);
    }

    /**
     * Send a reset email password to verified email attached to the users loginId.
     *
     * @param loginId       User login id.
     * @param gigyaCallback Response listener callback.
     */
    public void forgotPassword(String loginId, GigyaCallback<GigyaApiResponse> gigyaCallback) {
        GigyaLogger.debug(LOG_TAG, "forgotPassword: with " + loginId);
        _businessApiService.forgotPassword(loginId, gigyaCallback);
    }

    /**
     * Add a social connection to existing account.
     *
     * @param socialProvider Social provider identifier.
     * @param loginCallback  Response listener callback.
     */
    public void addConnection(@GigyaDefinitions.Providers.SocialProvider String socialProvider, GigyaLoginCallback<T> loginCallback) {
        GigyaLogger.debug(LOG_TAG, "addConnection: with " + socialProvider);
        _businessApiService.addConnection(socialProvider, loginCallback);
    }

    /**
     * Remove a social connection from an existing account.
     *
     * @param socialProvider Social provider identifier.
     * @param gigyaCallback  Response listener callback.
     */
    public void removeConnection(@GigyaDefinitions.Providers.SocialProvider String socialProvider, GigyaCallback<GigyaApiResponse> gigyaCallback) {
        GigyaLogger.debug(LOG_TAG, "removeConnection: with " + socialProvider);
        _businessApiService.removeConnection(socialProvider, gigyaCallback);
    }

    // TODO: #baryo should Gigya inherit from BusinessApiService?

    //endregion

    //region NATIVE LOGIN

    /**
     * Present social login selection list.
     *
     * @param providers          List of selected social providers {@link GigyaDefinitions.Providers.SocialProvider}.
     * @param params             Request parameters.
     * @param gigyaLoginCallback Login response callback.
     */
    public void socialLoginWith(@GigyaDefinitions.Providers.SocialProvider List<String> providers,
                                Map<String, Object> params, final GigyaLoginCallback<T> gigyaLoginCallback) {
        if (params == null) {
            params = new HashMap<>();
        }
        GigyaLogger.debug(LOG_TAG, "socialLoginWith: with parameters:\n" + params.toString());
        _presenter.showNativeLoginProviders(providers, _businessApiService, params, gigyaLoginCallback);
    }

    //endregion

    //region PLUGINS

    /**
     * Show Gigya ScreenSets flow using the PluginFragment.
     * UI will be presented via WebView.
     *
     * @param screensSet          Main ScreensSet group identifier
     * @param fullScreen          Show in fullscreen mode.
     * @param params              ScreensSet flow parameters.
     * @param gigyaPluginCallback Plugin callback.
     */
    public void showScreenSet(final String screensSet, boolean fullScreen, final Map<String, Object> params, final GigyaPluginCallback<T> gigyaPluginCallback) {
        params.put("screenSet", screensSet);
        GigyaLogger.debug(LOG_TAG, "showPlugin: " + PluginFragment.PLUGIN_SCREENSETS + ", with parameters:\n" + params.toString());
        _presenter.showPlugin(false, PluginFragment.PLUGIN_SCREENSETS, fullScreen, params, gigyaPluginCallback);
    }

    /**
     * Show Comments ScreenSets.
     *
     * @param params              Comments ScreenSet flow parameters.
     * @param fullScreen          Show in fullscreen mode.
     * @param gigyaPluginCallback Plugin callback.
     */
    // TODO: 16/04/2019 Not available in beta.
    private void showComments(Map<String, Object> params, boolean fullScreen, final GigyaPluginCallback<T> gigyaPluginCallback) {
        GigyaLogger.debug(LOG_TAG, "showPlugin: " + PluginFragment.PLUGIN_COMMENTS + ", with parameters:\n" + params.toString());
        _presenter.showPlugin(false, PluginFragment.PLUGIN_COMMENTS, fullScreen, params, gigyaPluginCallback);
    }

    //endregion
}
