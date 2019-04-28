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

import com.gigya.android.sdk.account.AccountService;
import com.gigya.android.sdk.account.IAccountService;
import com.gigya.android.sdk.api.ApiService;
import com.gigya.android.sdk.api.BusinessApiService;
import com.gigya.android.sdk.api.IApiService;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.encryption.ISecureKey;
import com.gigya.android.sdk.encryption.SessionKey;
import com.gigya.android.sdk.encryption.SessionKeyLegacy;
import com.gigya.android.sdk.interruption.IInterruptionsHandler;
import com.gigya.android.sdk.interruption.InterruptionHandler;
import com.gigya.android.sdk.model.account.GigyaAccount;
import com.gigya.android.sdk.model.account.SessionInfo;
import com.gigya.android.sdk.network.GigyaApiResponse;
import com.gigya.android.sdk.network.adapter.IRestAdapter;
import com.gigya.android.sdk.network.adapter.RestAdapter;
import com.gigya.android.sdk.persistence.IPersistenceService;
import com.gigya.android.sdk.persistence.PersistenceService;
import com.gigya.android.sdk.providers.IProviderFactory;
import com.gigya.android.sdk.providers.ProviderFactory;
import com.gigya.android.sdk.providers.provider.IProvider;
import com.gigya.android.sdk.session.ISessionService;
import com.gigya.android.sdk.session.ISessionVerificationService;
import com.gigya.android.sdk.session.SessionService;
import com.gigya.android.sdk.session.SessionVerificationService;
import com.gigya.android.sdk.ui.IPresenter;
import com.gigya.android.sdk.ui.Presenter;
import com.gigya.android.sdk.ui.plugin.IWebBridgeFactory;
import com.gigya.android.sdk.ui.plugin.IWebViewFragmentFactory;
import com.gigya.android.sdk.ui.plugin.PluginFragment;
import com.gigya.android.sdk.ui.plugin.WebBridgeFactory;
import com.gigya.android.sdk.ui.plugin.WebViewFragmentFactory;

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

    private static final String LOG_TAG = "Gigya";

    public static final String VERSION = "android_4.0.0_beta_1";

    @SuppressLint("StaticFieldLeak")
    private static Gigya INSTANCE;

    /**
     * SDK main configuration structure.
     */
    private Config _config = new Config();

    //region DEPENDENCY INJECTION

    private IoCContainer ioCContainer;

    private static IoCContainer registerDependencies(Context context) {
        IoCContainer container = new IoCContainer();
        container.bind(Context.class, context); // Concrete.
        container.bind(Config.class, Config.class, true); // Concrete.
        container.bind(IoCContainer.class, container);
        container.bind(IRestAdapter.class, RestAdapter.class, true);
        container.bind(IApiService.class, ApiService.class, false);
        container.bind(ISecureKey.class, Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 ? SessionKey.class
                : SessionKeyLegacy.class, true);
        container.bind(IPersistenceService.class, PersistenceService.class, false);
        container.bind(ISessionService.class, SessionService.class, true);
        container.bind(IAccountService.class, AccountService.class, true);
        container.bind(ISessionVerificationService.class, SessionVerificationService.class, true);
        container.bind(IProviderFactory.class, ProviderFactory.class, false);
        container.bind(IBusinessApiService.class, BusinessApiService.class, true);
        container.bind(IWebBridgeFactory.class, WebBridgeFactory.class, false);
        container.bind(IWebViewFragmentFactory.class, WebViewFragmentFactory.class, false);
        container.bind(IPresenter.class, Presenter.class, false);
        container.bind(IInterruptionsHandler.class, InterruptionHandler.class, true);
        return container;
    }

    // Undocumented public accessor.
    @Nullable
    public <C> C getComponent(Class<C> type) {
        try {
            return ioCContainer.get(type);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    //endregion

    @SuppressWarnings("unchecked")
    protected Gigya(@NonNull Context context, Class<T> accountScheme, IoCContainer container) {
        // Setup dependencies.
        ioCContainer = container;
        registerActivityLifecycleCallbacks(context);
        // Update account manager with scheme.
        try {
            _config = container.get(Config.class);
            final IAccountService<T> accountService = ioCContainer.get(IAccountService.class);
            accountService.setAccountScheme(accountScheme);
            final ISessionService sessionService = ioCContainer.get(ISessionService.class);
            sessionService.load();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        init();
    }

    /*
    Simplified instance getter for use only after calling getInstance(Context context) at least once.
     */
    @SuppressWarnings("unchecked")
    public static synchronized Gigya<? extends GigyaAccount> getInstance() {
        if (INSTANCE == null) {
            // Log error.
            GigyaLogger.error(LOG_TAG, "Gigya instance not initialized properly!" +
                    " Make sure to call Gigya getInstance(Context appContext) at least once before trying to reference The Gigya instance");
            return null;
        }
        return INSTANCE;
    }

    /*
    Simplified instance getter.
     */
    public static synchronized Gigya<GigyaAccount> getInstance(Context appContext) {
        return Gigya.getInstance(appContext, GigyaAccount.class);
    }

    public Context getContext() {
        try {
            return ioCContainer.get(Context.class);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    //region INITIALIZE

    /**
     * Gigya default api domain.
     */
    private static final String DEFAULT_API_DOMAIN = "us1.gigya.com";

    /*
    Generic account type instance getter.
     */
    @SuppressWarnings("unchecked")
    public static synchronized <V extends GigyaAccount> Gigya<V> getInstance(Context context, @NonNull Class<V> accountClazz) {
        if (INSTANCE == null) {
            // create container and register here.
            IoCContainer container = registerDependencies(context);
            INSTANCE = new Gigya(context, accountClazz, container);
        }
        // Check scheme. If already set log an error.
        final IAccountService<V> accountService = (IAccountService<V>) INSTANCE.getComponent(AccountService.class);
        if (accountService != null) {
            final Class scheme = accountService.getAccountScheme();
            if (scheme != accountClazz) {
                GigyaLogger.error(LOG_TAG, "Scheme already set in previous initialization.\nSDK does not allow to override a set scheme.");
            }
        }
        return INSTANCE;
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
        try {
            // Will load configuration fields only if none have yet to be set.
            final Context context = ioCContainer.get(Context.class);
            if (_config.getApiKey() == null && context != null) {
                // Try to from assets JSON file,
                Config dynamicConfig = Config.loadFromJson(context);
                if (dynamicConfig == null) {
                    dynamicConfig = Config.loadFromManifest(context);
                }
                _config.updateWith(dynamicConfig);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // Set next account invalidation timestamp if available.
        if (_config != null && _config.getAccountCacheTime() != 0) {
            try {
                final IAccountService accountService = ioCContainer.get(IAccountService.class);
                accountService.nextAccountInvalidationTimestamp();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    //endregion

    //region LIFECYCLE CALLBACKS

    /**
     * Attaching the SDK to the application lifecycle in order to distinguish foreground/background/resumed states.
     */
    private void registerActivityLifecycleCallbacks(Context context) {
        if (!(context instanceof Application)) {
            GigyaLogger.error(LOG_TAG, "SDK initialized with the wrong context. Please make sure you have initialized the SDK using the applicationContext");
            return;
        }
        ((Application) context).registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {

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
                    if (isLoggedIn()) {
                        // Will start session countdown timer if the current session contains an expiration time.
                        try {
                            final ISessionService sessionService = ioCContainer.get(ISessionService.class);
                            sessionService.startSessionCountdownTimerIfNeeded();
                            // Session verification is only relevant when user is logged in.
                            final ISessionVerificationService sessionVerificationService = ioCContainer.get(ISessionVerificationService.class);
                            sessionVerificationService.start();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
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
                    try {
                        final ISessionService sessionService = ioCContainer.get(ISessionService.class);
                        sessionService.cancelSessionCountdownTimer();
                        final ISessionVerificationService sessionVerificationService = ioCContainer.get(ISessionVerificationService.class);
                        sessionVerificationService.stop();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
                // Stub.
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                if (--activityReferences == 0 && !isActivityChangingConfigurations) {
                    // Flush the Presenter statics just in case. When all activities have been destroyed.
                    Presenter.flush();
                }
            }
        });
    }

    //endregion

    //region PUBLIC INTERFACING

    /**
     * Update interruption handling.
     * By default, the Gigya SDK will handle various API interruptions to allow simple resolving of certain common errors.
     * Setting interruptions to FALSE will force the end user to handle his own errors.
     *
     * @param sdkHandles False if manually handling all errors.
     */
    public void handleInterruptions(boolean sdkHandles) {
        try {
            final IInterruptionsHandler interruptionsResolver = ioCContainer.get(IInterruptionsHandler.class);
            interruptionsResolver.setEnabled(sdkHandles);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Return SDK interruptions state.
     * if TRUE, interruption handling will be optional via the GigyaLoginCallback.
     */
    public boolean interruptionsEnabled() {
        try {
            final IInterruptionsHandler interruptionsResolver = ioCContainer.get(IInterruptionsHandler.class);
            interruptionsResolver.isEnabled();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return true;
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
        try {
            final IBusinessApiService service = ioCContainer.get(IBusinessApiService.class);
            service.send(api, params, RestAdapter.POST, GigyaApiResponse.class, gigyaCallback);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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
        try {
            final IBusinessApiService service = ioCContainer.get(IBusinessApiService.class);
            service.send(api, params, requestMethod, clazz, gigyaCallback);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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
        try {
            final ISessionService sessionService = ioCContainer.get(ISessionService.class);
            return sessionService.getSession();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Check if we currently have a valid session.
     */
    public boolean isLoggedIn() {
        try {
            final ISessionService sessionService = ioCContainer.get(ISessionService.class);
            return sessionService.isValid();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    /**
     * Logout of Gigya services.
     * This will clean all session related data persistence.
     */
    @SuppressWarnings("deprecation")
    @SuppressLint("ObsoleteSdkInt")
    public void logout() {
        GigyaLogger.debug(LOG_TAG, "logout: ");
        try {
            final IBusinessApiService baService = ioCContainer.get(IBusinessApiService.class);
            baService.logout(null);
            final ISessionService sessionService = ioCContainer.get(ISessionService.class);
            sessionService.clear(true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            // Clearing cached cookies.
            final Context context = ioCContainer.get(Context.class);
            CookieManager cookieManager = CookieManager.getInstance();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                cookieManager.flush();
            } else {
                CookieSyncManager.createInstance(context);
                cookieManager.removeAllCookie();
            }

            // Logout of social providers...
            final IPersistenceService psService = ioCContainer.get(IPersistenceService.class);
            final IProviderFactory providerFactory = ioCContainer.get(IProviderFactory.class);
            final Set<String> usedProviders = psService.getSocialProviders();
            if (!usedProviders.isEmpty()) {
                for (String name : usedProviders) {
                    final IProvider provider = providerFactory.providerFor(name, null, null);
                    if (provider.getName().equals(name)) {
                        // Make sure were not getting the web view provider.
                        provider.logout(context);
                    }
                }
                psService.removeSocialProviders();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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
        try {
            final IBusinessApiService<T> bService = ioCContainer.get(IBusinessApiService.class);
            bService.login(params, gigyaLoginCallback);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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
        try {
            final Context context = ioCContainer.get(Context.class);
            final IBusinessApiService<T> service = ioCContainer.get(IBusinessApiService.class);
            service.login(context, socialProvider, params, gigyaLoginCallback);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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
        try {
            if (overrideCache) {
                final IAccountService accountService = ioCContainer.get(IAccountService.class);
                accountService.setAccountOverrideCache(overrideCache);
            }
            final IBusinessApiService<T> service = ioCContainer.get(IBusinessApiService.class);
            service.getAccount(gigyaCallback);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Set account info
     *
     * @param account       Updated account object.
     * @param gigyaCallback Response listener callback.
     */
    public void setAccount(T account, GigyaCallback<T> gigyaCallback) {
        GigyaLogger.debug(LOG_TAG, "setAccount: ");
        try {
            final IBusinessApiService<T> service = ioCContainer.get(IBusinessApiService.class);
            service.setAccount(account, gigyaCallback);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Set account info given update parameters.
     *
     * @param params        Updated account parameters.
     * @param gigyaCallback Response listener callback.
     */
    public void setAccount(Map<String, Object> params, GigyaCallback<T> gigyaCallback) {
        GigyaLogger.debug(LOG_TAG, "setAccount: with params");
        try {
            final IBusinessApiService<T> service = ioCContainer.get(IBusinessApiService.class);
            service.setAccount(params, gigyaCallback);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Request verify login given account UID/
     *
     * @param UID           Account UID identifier.
     * @param gigyaCallback Response listener callback.
     */
    public void verifyLogin(String UID, GigyaCallback<T> gigyaCallback) {
        GigyaLogger.debug(LOG_TAG, "verifyLogin: for UID = " + UID);
        try {
            final IBusinessApiService<T> service = ioCContainer.get(IBusinessApiService.class);
            service.verifyLogin(UID, gigyaCallback);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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
        try {
            final IBusinessApiService<T> service = ioCContainer.get(IBusinessApiService.class);
            service.register(params, callback);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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
        try {
            final IBusinessApiService<T> service = ioCContainer.get(IBusinessApiService.class);
            service.forgotPassword(loginId, gigyaCallback);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Add a social connection to existing account.
     *
     * @param socialProvider Social provider identifier.
     * @param loginCallback  Response listener callback.
     */
    public void addConnection(@GigyaDefinitions.Providers.SocialProvider String socialProvider, GigyaLoginCallback<T> loginCallback) {
        GigyaLogger.debug(LOG_TAG, "addConnection: with " + socialProvider);
        try {
            final Context context = ioCContainer.get(Context.class);
            final IBusinessApiService<T> service = ioCContainer.get(IBusinessApiService.class);
            service.addConnection(context, socialProvider, loginCallback);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Remove a social connection from an existing account.
     *
     * @param socialProvider Social provider identifier.
     * @param gigyaCallback  Response listener callback.
     */
    public void removeConnection(@GigyaDefinitions.Providers.SocialProvider String socialProvider, GigyaCallback<GigyaApiResponse> gigyaCallback) {
        GigyaLogger.debug(LOG_TAG, "removeConnection: with " + socialProvider);
        try {
            final IBusinessApiService<T> service = ioCContainer.get(IBusinessApiService.class);
            service.removeConnection(socialProvider, gigyaCallback);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

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
        try {
            final IPresenter presenter = ioCContainer.get(IPresenter.class);
            final IBusinessApiService baService = ioCContainer.get(IBusinessApiService.class);
            presenter.showNativeLoginProviders(providers, baService, params, gigyaLoginCallback);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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
    public void showScreenSets(final String screensSet, boolean fullScreen, final Map<String, Object> params, final GigyaPluginCallback<T> gigyaPluginCallback) {
        params.put("screenSet", screensSet);
        GigyaLogger.debug(LOG_TAG, "showPlugin: " + PluginFragment.PLUGIN_SCREENSETS + ", with parameters:\n" + params.toString());
        try {
            final IPresenter presenter = ioCContainer.get(IPresenter.class);
            presenter.showPlugin(false, PluginFragment.PLUGIN_SCREENSETS, fullScreen, params, gigyaPluginCallback);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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
        try {
            final IPresenter presenter = ioCContainer.get(IPresenter.class);
            presenter.showPlugin(false, PluginFragment.PLUGIN_COMMENTS, fullScreen, params, gigyaPluginCallback);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    //endregion
}
