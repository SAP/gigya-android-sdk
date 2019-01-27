package com.gigya.android.sdk.ui.provider;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.gigya.android.sdk.ApiManager;
import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.PersistenceManager;
import com.gigya.android.sdk.log.GigyaLogger;
import com.gigya.android.sdk.login.LoginProvider;
import com.gigya.android.sdk.login.LoginProviderFactory;
import com.gigya.android.sdk.login.provider.WebViewLoginProvider;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.model.SessionInfo;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.ui.GigyaPresenter;
import com.gigya.android.sdk.ui.HostActivity;
import com.gigya.android.sdk.ui.WebViewFragment;
import com.gigya.android.sdk.utils.UrlUtils;

import java.util.HashMap;
import java.util.Map;

public class GigyaLoginPresenter extends GigyaPresenter {

    private static final String LOG_TAG = "GigyaLoginPresenter";

    private static final String REDIRECT_URI = "gsapi://result/";

    public GigyaLoginPresenter(ApiManager apiManager, PersistenceManager persistenceManager) {
        super(apiManager, persistenceManager);
    }

    public interface LoginPresentationCallbacks {
        void onProviderSelected(LoginProvider loginProvider);

        void onCancelled();
    }

    public <T> void showNativeLoginProviders(final Context context,
                                             final Configuration configuration,
                                             final Map<String, Object> params,
                                             final LoginProvider.LoginProviderTrackerCallback trackerCallback,
                                             final LoginPresentationCallbacks presentationCallbacks,
                                             final GigyaCallback<T> callback) {
        /*
        Url generation must be out of the lifecycle callback scope. Otherwise we will have a serializable error.
         */
        final String url = getPresentationUrl(configuration, params, "login");
        HostActivity.present(context, new HostActivity.HostActivityLifecycleCallbacks() {
            @Override
            public void onCreate(final AppCompatActivity activity, @Nullable Bundle savedInstanceState) {
                Bundle args = new Bundle();
                args.putString(ProviderFragment.ARG_TITLE, "Sign in");
                args.putString(ProviderFragment.ARG_URL, url);
                args.putString(ProviderFragment.ARG_REDIRECT_PREFIX, "gsapi");
                ProviderFragment.present(activity, args, new WebViewFragment.WebViewFragmentLifecycleCallbacks() {

                    @Override
                    public void onWebViewResult(Map<String, Object> result) {
                        /* Handle result. */
                        final String provider = (String) result.get("provider");
                        if (provider == null) {
                            /* Internal check. Should not happen if SDK implementation is correct. */
                            return;
                        }
                        login(provider);
                    }

                    @Override
                    public void onWebViewCancel() {
                        /* User cancelled WebView. */
                        presentationCallbacks.onCancelled();
                    }

                    /* Determine if we need to fetch SDK configuration. */
                    LoginProvider.LoginProviderConfigCallback _configCallback = new LoginProvider.LoginProviderConfigCallback() {
                        @Override
                        public void onConfigurationRequired(final LoginProvider provider) {
                            _apiManager.loadConfig(new Runnable() {
                                @Override
                                public void run() {
                                    if (configuration.isSynced()) {
                                        /* Update provider client id if available */
                                        final String providerClientId = configuration.getAppIds().get(provider.getName());
                                        if (providerClientId != null) {
                                            provider.updateProviderClientId(providerClientId);
                                        }
                                        if (activity != null) {
                                            activity.finish();
                                        }
                                        provider.login(context, params);
                                    }
                                }
                            });
                        }
                    };

                    /* Login provider operation callbacks. */
                    LoginProvider.LoginProviderCallbacks _loginCallbacks = new LoginProvider.LoginProviderCallbacks() {

                        @Override
                        public void onCanceled() {
                            presentationCallbacks.onCancelled();
                        }

                        @Override
                        public void onProviderLoginSuccess(final LoginProvider provider, String providerSessions) {
                            GigyaLogger.debug(LOG_TAG, "onProviderLoginSuccess: provider = "
                                    + provider + ", providerSessions = " + providerSessions);
                            /* Call intermediate load to give the client the option to trigger his own progress indicator */
                            callback.onIntermediateLoad();

                            _apiManager.notifyLogin(providerSessions, callback, new Runnable() {
                                @Override
                                public void run() {
                                    /* Safe to say this is the current selected provider. */
                                    presentationCallbacks.onProviderSelected(provider);
                                    _persistenceManager.onLoginProviderUpdated(provider.getName());
                                }
                            });
                        }

                        @Override
                        public void onProviderSession(final LoginProvider provider, SessionInfo sessionInfo) {
                            /* Login process via Web has generated a new session. */

                            /* Call intermediate load to give the client the option to trigger his own progress indicator */
                            callback.onIntermediateLoad();

                            /* Call notifyLogin to complete sign in process.*/
                            _apiManager.notifyLogin(sessionInfo, callback, new Runnable() {
                                @Override
                                public void run() {
                                    _persistenceManager.onLoginProviderUpdated(provider.getName());
                                }
                            });
                        }

                        @Override
                        public void onProviderLoginFailed(String provider, String error) {
                            GigyaLogger.debug(LOG_TAG, "onProviderLoginFailed: provider = "
                                    + provider + ", error =" + error);
                            callback.onError(GigyaError.errorFrom(error));
                        }
                    };

                    private void login(final String provider) {
                        params.put("provider", provider);
                        LoginProvider loginProvider = LoginProviderFactory.providerFor(context, configuration,
                                provider, _loginCallbacks, trackerCallback);

                        if (loginProvider instanceof WebViewLoginProvider && !configuration.hasGMID()) {
                            /* WebView Provider must have basic config fields. */
                            _configCallback.onConfigurationRequired(loginProvider);
                            return;
                        }
                        if (loginProvider.clientIdRequired() && !configuration.isSynced()) {
                            _configCallback.onConfigurationRequired(loginProvider);
                            return;
                        }

                        /* Login provider selected. */
                        presentationCallbacks.onProviderSelected(loginProvider);

                        /* Okay to release activity. */
                        activity.finish();

                        if (configuration.isSynced()) {
                            /* Update provider client id if available */
                            final String providerClientId = configuration.getAppIds().get(provider);
                            if (providerClientId != null) {
                                loginProvider.updateProviderClientId(providerClientId);
                            }
                        }

                        loginProvider.login(context, params);
                    }
                });
            }
        });
    }

    //region Utilities

    @SuppressWarnings("ConstantConditions")
    private static String getPresentationUrl(Configuration configuration, Map<String, Object> params, String requestType) {
        /* Setup parameters. */
        final Map<String, Object> urlParams = new HashMap<>();
        urlParams.put("apiKey", configuration.getApiKey());
        urlParams.put("requestType", requestType);
        if (params.containsKey("enabledProviders")) {
            urlParams.put("enabledProviders", params.get("enabledProviders"));
        }
        if (params.containsKey("disabledProviders")) {
            urlParams.put("disabledProviders", params.get("disabledProviders"));
        }
        if (params.containsKey("lang")) {
            urlParams.put("lang", params.get("lang"));
        }
        if (params.containsKey("cid")) {
            urlParams.put("cid", params.get("cid"));
        }
        urlParams.put("sdk", Gigya.VERSION);
        urlParams.put("redirect_uri", REDIRECT_URI);
        final String qs = UrlUtils.buildEncodedQuery(urlParams);

        // Setup url.
        final String endpoint = "gs/mobile/loginui.aspx";
        final String protocol = "https";
        final String domainPrefix = "socialize";
        return String.format("%s://%s.%s/%s?%s", protocol, domainPrefix, configuration.getApiDomain(), endpoint, qs);
    }

    //endregion
}
