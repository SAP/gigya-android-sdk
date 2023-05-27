package com.gigya.android.sdk;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gigya.android.sdk.account.GigyaAccountClass;
import com.gigya.android.sdk.account.GigyaAccountConfig;
import com.gigya.android.sdk.account.IAccountService;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.auth.IWebAuthnService;
import com.gigya.android.sdk.containers.GigyaContainer;
import com.gigya.android.sdk.containers.IoCContainer;
import com.gigya.android.sdk.interruption.IInterruptionResolverFactory;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.adapter.RestAdapter;
import com.gigya.android.sdk.providers.IProviderFactory;
import com.gigya.android.sdk.providers.provider.Provider;
import com.gigya.android.sdk.reporting.IReportingService;
import com.gigya.android.sdk.reporting.ReportingManager;
import com.gigya.android.sdk.schema.GigyaSchema;
import com.gigya.android.sdk.session.ISessionService;
import com.gigya.android.sdk.session.ISessionVerificationService;
import com.gigya.android.sdk.session.SessionInfo;
import com.gigya.android.sdk.session.SessionStateObserver;
import com.gigya.android.sdk.ui.IPresenter;
import com.gigya.android.sdk.ui.plugin.GigyaPluginFragment;
import com.gigya.android.sdk.ui.plugin.IGigyaWebBridge;
import com.gigya.android.sdk.utils.EnvUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Gigya SDK main interface.
 * Provides access to the Gigya services.
 *
 * @param <T> Generic account scheme. Extended from base GigyaAccount model.
 */
public class Gigya<T extends GigyaAccount> {

    //region static
    public static final String VERSION = "7.0.2";

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

    /**
     * Use this flag when you want to apply FLAG_SECURE to all SDK activities.
     * These include the screen-set running HostActivity and the WebLoginActivity used for
     * social login flows.
     * Default is FALSE.
     */
    public static void secureActivityWindow(boolean secure) {
        try {
            getContainer().get(Config.class).setSecureActivities(secure);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Activate SDK error reporting (inactive by default).
     * Reporting is used internally by the SDK to track critical errors within the SDK core flows.
     *
     * @param active True to activate.
     */
    public static void setErrorReporting(boolean active) {
        try {
            getContainer().get(IReportingService.class).setErrorReporting(active);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void setApplication(Application appContext) {
        EnvUtils.checkGson();
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
        EnvUtils.checkGson();
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
        EnvUtils.checkGson();
        if (INSTANCE == null) {
            IoCContainer container = getContainer();
            container.bind(GigyaAccountClass.class, new GigyaAccountClass(accountClazz));

            try {
                INSTANCE = container.createInstance(Gigya.class);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Error creating Gigya SDK (did you forget to Gigya.setApplication or missing apiKey?)");
            }
        }
        // Check scheme. If already set log an error.
        final Class schema = INSTANCE.getAccountSchema();
        if (schema != accountClazz) {
            throw new RuntimeException("Scheme already set in previous initialization.\nSDK does not allow to override a set scheme.");
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
    final private IInterruptionResolverFactory _interruptionResolverFactory;
    final private IPresenter<T> _presenter;
    final private IProviderFactory _providerFactory;
    final private IoCContainer _container;
    final private IWebAuthnService _webAuthnService;

    protected Gigya(
            @NonNull Application context,
            Config config,
            ConfigFactory configFactory,
            ISessionService sessionService,
            IAccountService<T> accountService,
            IBusinessApiService<T> businessApiService,
            ISessionVerificationService sessionVerificationService,
            IInterruptionResolverFactory interruptionsHandler,
            IPresenter<T> presenter,
            IProviderFactory providerFactory,
            IoCContainer container,
            IWebAuthnService webAuthnService) {
        // Setup dependencies.
        _context = context;
        _config = config;
        _configFactory = configFactory;
        _sessionService = sessionService;
        _accountService = accountService;
        _businessApiService = businessApiService;
        _sessionVerificationService = sessionVerificationService;
        _interruptionResolverFactory = interruptionsHandler;
        _presenter = presenter;
        _providerFactory = providerFactory;
        _container = container;
        _webAuthnService = webAuthnService;

        // Setup sdk
        _sessionService.load();
        init(false);

        // Must be registered following the init call. Dependent on full parsed config.
        _sessionVerificationService.registerActivityLifecycleCallbacks();
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
    public void init(@NonNull String apiKey) {
        init(apiKey, DEFAULT_API_DOMAIN);
    }

    /**
     * Explicitly initialize the SDK.
     *
     * @param apiKey    Client API-KEY
     * @param apiDomain Request Domain.
     */
    public void init(@NonNull String apiKey, @NonNull String apiDomain) {
        // Override existing configuration when applied explicitly.
        _config.updateWith(apiKey, apiDomain);
        init(true);
    }

    /**
     * Explicitly initialize the SDK With additional CNAME parameter.
     *
     * @param apiKey Client API-KEY
     * @param apiDomain Request Domain.
     * @param cname  CNAME.
     */
    public void init(@NonNull String apiKey, @NonNull String apiDomain, @NonNull String cname) {
        // Override existing configuration when applied explicitly.
        _config.updateWith(apiKey, apiDomain, cname);
        init(true);
    }

    /**
     * Implicitly initialize the SDK.
     * Available Options:
     * - read JSON assets file.
     * - parse application manifest meta data tags.
     * For explicit setting see {@link #init(String, String)} method.
     */
    private void init(boolean explicit) {
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

        if (explicit) {
            if (_config.getApiKey() == null || _config.getApiKey().isEmpty()) {
                GigyaLogger.error(LOG_TAG, "Failed to set the SDK Api-Key. Please verify you have correctly initialized the SDK.");
                throw new RuntimeException("Failed to set the SDK Api-Key. Please verify you have correctly initialized the SDK.");
            }
        }

        // Request SDK configuration (blocking).
        if (_config.getApiKey() != null) {
            _businessApiService.getSDKConfig();
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
        _interruptionResolverFactory.setEnabled(sdkHandles);
    }

    /**
     * Return SDK interruptions state.
     * if TRUE, interruption handling will be optional via the GigyaLoginCallback.
     */
    public boolean interruptionsEnabled() {
        return _interruptionResolverFactory.isEnabled();
    }

    //endregion

    //region CONFIG

    /**
     * Set Account configuration fields
     * <p>
     * These configuration fields will be used by default for all relevant SDK calls.
     *
     * @param gigyaAccountConfig AccountConfig object.
     */
    public void setAccountConfig(GigyaAccountConfig gigyaAccountConfig) {
        _config.setGigyaAccountConfig(gigyaAccountConfig);
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
    public void send(String api, Map<String, Object> params, GigyaCallback<GigyaApiResponse> gigyaCallback) {
        _businessApiService.send(api, params, RestAdapter.HttpMethod.POST.intValue(), GigyaApiResponse.class, gigyaCallback);
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
     * Manually set the current session.
     * Setting a session manually will update the current session persistence state and login state.
     *
     * @param session SessionInfo instance.
     */
    public void setSession(@NonNull SessionInfo session) {
        _sessionService.setSession(session);
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
    public void logout() {
        logout(null);
    }

    /**
     * Logout of Gigya services.
     * This will clean all session related data persistence.
     *
     * @param gigyaCallback Response listener callback.
     */
    public void logout(GigyaCallback<GigyaApiResponse> gigyaCallback) {
        GigyaLogger.debug(LOG_TAG, "logout: ");

        _businessApiService.logout(gigyaCallback);

        _sessionService.cancelSessionCountdownTimer();
        _sessionService.clear(true);
        _sessionService.clearCookiesOnLogout();

        // Make sure account cache is also cleared.
        _accountService.invalidateAccount();

        _sessionVerificationService.stop();

        _providerFactory.logoutFromUsedSocialProviders();
    }

    /**
     * Enable/Disable clearing cookies from WebView on logout.
     * Default is set to true.
     *
     * @param clear False to disable clearing cookies from WebView instances on logout.
     */
    public void setClearCookies(boolean clear) {
        _sessionService.setClearCookies(clear);
    }

    //endregion

    //region SESSION OBSERVERS

    public void registerSessionExpirationObserver(SessionStateObserver observer) {
        _sessionService.registerExpirationObserver(observer);
    }

    public void unregisterSessionExpirationObserver(SessionStateObserver observer) {
        _sessionService.removeExpirationObserver(observer);
    }

    public void registerSessionVerificationObserver(SessionStateObserver observer) {
        _sessionVerificationService.registerObserver(observer);
    }

    public void unregisterSessionVerificationObserver(SessionStateObserver observer) {
        _sessionVerificationService.removeObserver(observer);
    }

    //endregion

    //region BUSINESS APIS


    /**
     * Validate session endpoint.
     * Please use this over getAccountInfo to check session validation state.
     *
     * @param gigyaCallback Response listener callback.
     */
    public void verifySession(GigyaCallback<GigyaApiResponse> gigyaCallback) {
        GigyaLogger.debug(LOG_TAG, "isSessionValid");
        _businessApiService.verifySession(gigyaCallback);
    }

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
        login(params, gigyaCallback);
    }

    /**
     * Login with provided id and password.
     *
     * @param loginId       LoginID.
     * @param password      Login password.
     * @param params        additional parameter map.
     * @param gigyaCallback Response listener callback.
     */
    public void login(String loginId, String password, @NonNull Map<String, Object> params, GigyaLoginCallback<T> gigyaCallback) {
        params.put("loginID", loginId);
        params.put("password", password);
        login(params, gigyaCallback);
    }

    /**
     * Login with given parameters.
     *
     * @param params             parameters map.
     * @param gigyaLoginCallback Login response callback.
     */
    public void login(@NonNull Map<String, Object> params, final GigyaLoginCallback<T> gigyaLoginCallback) {
        GigyaLogger.debug(LOG_TAG, "login: with params = " + params.toString());
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
     * Single sign on login.
     *
     * @param params             Additional parameters map.
     * @param gigyaLoginCallback Login response callback.
     */
    public void sso(Map<String, Object> params, GigyaLoginCallback<T> gigyaLoginCallback) {
        GigyaLogger.debug(LOG_TAG, "login: with SSO provider");
        _businessApiService.login(GigyaDefinitions.Providers.SSO, params, gigyaLoginCallback);
    }

    /**
     * Request account info.
     *
     * @param gigyaCallback Response listener callback.
     */
    public void getAccount(@NonNull GigyaCallback<T> gigyaCallback) {
        GigyaLogger.debug(LOG_TAG, "getAccount: ");
        getAccount(false, gigyaCallback);
    }

    /**
     * Request account info.
     *
     * @param invalidateCache Should override the account caching option. When set to true, the SDK will not cache the account object.
     * @param gigyaCallback   Response listener callback.
     */
    @SuppressWarnings("unused")
    public void getAccount(final boolean invalidateCache, GigyaCallback<T> gigyaCallback) {
        GigyaLogger.debug(LOG_TAG, "getAccount: overrideCache = " + invalidateCache);
        if (invalidateCache) {
            _accountService.invalidateAccount();
        }

        _businessApiService.getAccount(gigyaCallback);
    }

    /**
     * Request account info given parameters map.
     *
     * @param params        Request parameter map.
     * @param gigyaCallback Response listener callback.
     */
    public void getAccount(@NonNull final Map<String, Object> params, @NonNull GigyaCallback<T> gigyaCallback) {
        GigyaLogger.debug(LOG_TAG, "getAccount with params:\n" + params.toString());
        _businessApiService.getAccount(params, gigyaCallback);
    }

    /**
     * Request account info given parameters map.
     *
     * @param invalidateCache Should override the account caching option. When set to true, the SDK will not cache the account object.
     * @param params          Request parameter map.
     * @param gigyaCallback   Response listener callback.
     */
    public void getAccount(final boolean invalidateCache, @NonNull final Map<String, Object> params, @NonNull GigyaCallback<T> gigyaCallback) {
        GigyaLogger.debug(LOG_TAG, "getAccount with params:\n" + params.toString());
        if (invalidateCache) {
            _accountService.invalidateAccount();
        }
        _businessApiService.getAccount(params, gigyaCallback);
    }


    /**
     * Request account info given comma separated array of include parameters and comma separated array of profile extra fields.
     *
     * @param include            String[]  array.
     * @param profileExtraFields String[] array.
     * @param gigyaCallback      Response listener callback.
     * @deprecated Please use {@link #getAccount(boolean, Map, GigyaCallback)} method and add "include" and "extraProfileFields" accordingly.
     */
    @Deprecated
    public void getAccount(@NonNull final String[] include, @NonNull final String[] profileExtraFields, @NonNull GigyaCallback<T> gigyaCallback) {
        GigyaLogger.debug(LOG_TAG, "getAccount with include:\n" + Arrays.toString(include)
                + "\nand profileExtraFields:\n" + Arrays.toString(profileExtraFields));
        _businessApiService.getAccount(include, profileExtraFields, gigyaCallback);
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
     * Request verify login given account UID/
     *
     * @param UID           Account UID identifier.
     * @param params        Additional parameters.
     * @param gigyaCallback Response listener callback.
     */
    public void verifyLogin(String UID, Map<String, Object> params, GigyaCallback<T> gigyaCallback) {
        GigyaLogger.debug(LOG_TAG, "verifyLogin: for UID = " + UID);
        _businessApiService.verifyLogin(UID, params, gigyaCallback);
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
    public void register(String email, String password, @NonNull Map<String, Object> params, GigyaLoginCallback<T> callback) {
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
        final Map<String, Object> params = new HashMap<>();
        params.put("loginID", loginId);
        forgotPassword(params, gigyaCallback);
    }

    /**
     * Send a reset email password to verified email attached to the users loginId.
     *
     * @param params        Parameter map.
     * @param gigyaCallback Response listener callback.
     * @see <a href="https://developers.gigya.com/display/GD/accounts.resetPassword+REST">accounts.resetPassword REST</a> for available parameters.
     */
    public void forgotPassword(@NonNull Map<String, Object> params, GigyaCallback<GigyaApiResponse> gigyaCallback) {
        GigyaLogger.debug(LOG_TAG, "forgotPassword: with given parameters " + params.toString());
        _businessApiService.forgotPassword(params, gigyaCallback);
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
     * Add a social connection to existing account.
     *
     * @param socialProvider Social provider identifier.
     * @param loginCallback  Response listener callback.
     */
    public void addConnection(@GigyaDefinitions.Providers.SocialProvider String socialProvider, @NonNull Map<String, Object> params, GigyaLoginCallback<T> loginCallback) {
        GigyaLogger.debug(LOG_TAG, "addConnection: with " + socialProvider);
        _businessApiService.addConnection(socialProvider, params, loginCallback);
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

    /**
     * Login to with social provider when the provider session is available (obtained via specific provider login process).
     *
     * @param params             Parameter map.
     * @param gigyaLoginCallback Response listener callback.
     */
    public void notifySocialLogin(@NonNull Map<String, Object> params, GigyaLoginCallback<T> gigyaLoginCallback) {
        GigyaLogger.debug(LOG_TAG, "notifySocialLogin: with parameters: " + params.toString());
        _businessApiService.notifyNativeSocialLogin(params, gigyaLoginCallback, null);
    }

    //endregion

    //region NATIVE LOGIN

    /**
     * Custom setter for external provider package path.
     *
     * @param path Path to package where the external providers path are located.
     */
    public void setExternalProvidersPath(String path) {
        _providerFactory.setExternalProvidersPath(path);
    }

    /**
     * Request reference to used Gigya social provider.
     * Currently supported provider (GOOGLE, FACEBOOK, LINE, WECHAT).
     *
     * @param name Provider name.
     * @return Provider reference or null if not available.
     */
    @Nullable
    public Provider getUsedSocialProvider(String name) {
        return _providerFactory.usedProviderFor(name);
    }

    /**
     * Present social login selection list.
     *
     * @param providers          List of selected social providers {@link GigyaDefinitions.Providers.SocialProvider}.
     * @param params             Request parameters.
     * @param gigyaLoginCallback Login response callback.
     */
    public void socialLoginWith(@GigyaDefinitions.Providers.SocialProvider List<String> providers,
                                @NonNull Map<String, Object> params, final GigyaLoginCallback<T> gigyaLoginCallback) {
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
    public void showScreenSet(final String screensSet, boolean fullScreen, @NonNull final Map<String, Object> params, final GigyaPluginCallback<T> gigyaPluginCallback) {
        params.put("screenSet", screensSet);
        GigyaLogger.debug(LOG_TAG, "showPlugin: " + GigyaPluginFragment.PLUGIN_SCREENSETS + ", with parameters:\n" + params.toString());
        _presenter.showPlugin(false, GigyaPluginFragment.PLUGIN_SCREENSETS, fullScreen, params, gigyaPluginCallback);
    }

    /**
     * Show Gigya ScreenSets flow using the PluginFragment.
     * UI will be presented via WebView.
     *
     * @param screensSet          Main ScreensSet group identifier
     * @param obfuscate           Obfuscate WebBridge data.
     * @param fullScreen          Show in fullscreen mode.
     * @param params              ScreensSet flow parameters.
     * @param gigyaPluginCallback Plugin callback.
     */
    public void showScreenSet(final String screensSet, boolean obfuscate, boolean fullScreen, @NonNull final Map<String, Object> params, final GigyaPluginCallback<T> gigyaPluginCallback) {
        params.put("screenSet", screensSet);
        GigyaLogger.debug(LOG_TAG, "showPlugin: " + GigyaPluginFragment.PLUGIN_SCREENSETS + ", with parameters:\n" + params.toString());
        _presenter.showPlugin(obfuscate, GigyaPluginFragment.PLUGIN_SCREENSETS, fullScreen, params, gigyaPluginCallback);
    }

    /**
     * Update device information in server.
     * Device information includes: platform, manufacturer, os and push token.
     * Use this method manually if your flow requires to update the push service token.
     * Additional device info is generated at runtime.
     *
     * @param newPushToken New provided push token.
     */
    public void updateDeviceInfo(@NonNull final String newPushToken) {
        _businessApiService.updateDevice(newPushToken, new GigyaCallback<GigyaApiResponse>() {
            @Override
            public void onSuccess(GigyaApiResponse obj) {
                GigyaLogger.debug(LOG_TAG, "Successfully update push token. Persisting new token");
            }

            @Override
            public void onError(GigyaError error) {
                GigyaLogger.debug(LOG_TAG, "Failed to update device info.");
            }
        });
    }

    /**
     * Create an new instance of the GigyaWebBridge.
     *
     * @return GigyaWebBridge instance.
     */
    @SuppressWarnings("unchecked")
    public IGigyaWebBridge<T> createWebBridge() {
        try {
            return _container.get(IGigyaWebBridge.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            ReportingManager.get().error(VERSION, "core", "Unable to create new WebBridge instance");
            GigyaLogger.error(LOG_TAG, "Exception creating new WebBridge instance");
        }
        return null;
    }


    //endregion

    //region WEBAUTHN

    public IWebAuthnService WebAuthn() {
        return _webAuthnService;
    }

    //endregion

    //region MISC

    /**
     * This method checks whether a certain login identifier (username / email) is available.
     * A login identifier is available if it is unique in this user management system.
     *
     * @param loginId       The login identifier to check if available. Can be either a username or an email address.
     * @param gigyaCallback Response listener callback.
     */
    public void isAvailableLoginId(@NonNull final String loginId, @NonNull final GigyaCallback<Boolean> gigyaCallback) {
        GigyaLogger.debug(LOG_TAG, "lisAvailableLoginId: with id = " + loginId);
        _businessApiService.isAvailableLoginId(loginId, gigyaCallback);
    }

    /**
     * This method retrieves the schema of the Profile object and the Data object
     * (the site specific custom data object) in Gigya's Accounts Storage.
     *
     * @param gigyaCallback Response listener callback.
     */
    public void getSchema(@NonNull GigyaCallback<GigyaSchema> gigyaCallback) {
        GigyaLogger.debug(LOG_TAG, "getSchema: ");
        _businessApiService.getSchema(null, gigyaCallback);
    }

    /**
     * This method retrieves the schema of the Profile object and the Data object
     * (the site specific custom data object) in Gigya's Accounts Storage.
     *
     * @param params        Additional parameters.
     * @param gigyaCallback Response listener callback.
     */
    public void getSchema(@NonNull Map<String, Object> params, @NonNull GigyaCallback<GigyaSchema> gigyaCallback) {
        GigyaLogger.debug(LOG_TAG, "getSchema: ");
        _businessApiService.getSchema(params, gigyaCallback);
    }

    //endregion
}
