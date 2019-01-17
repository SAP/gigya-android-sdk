package com.gigya.android.sdk.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;

import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.login.LoginProvider;
import com.gigya.android.sdk.login.LoginProviderFactory;
import com.gigya.android.sdk.login.provider.WebViewLoginProvider;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.utils.UrlUtils;

import java.util.HashMap;
import java.util.Map;

public class GigyaLoginPresenter {

    private static final String TAG = "GigyaLoginPresenter";

    private static final String REDIRECT_URI = "gsapi://result/";

    //region HostActivity lifecycle callbacks tracking

    // TODO: 03/01/2019 When dropping support for <17 devices remove static references!!! Use Binder instead to attach the callbacks to the activity intent.

    private static SparseArray<HostActivity.HostActivityLifecycleCallbacks> lifecycleSparse = new SparseArray<>();

    static int addLifecycleCallbacks(HostActivity.HostActivityLifecycleCallbacks callbacks) {
        int id = callbacks.hashCode();
        lifecycleSparse.append(id, callbacks);
        return id;
    }

    static HostActivity.HostActivityLifecycleCallbacks getCallbacks(int id) {
        return lifecycleSparse.get(id);
    }

    static void flushLifecycleCallbacks(int id) {
        lifecycleSparse.remove(id);
    }

    public static void flush() {
        lifecycleSparse.clear();
        System.gc();
    }

    //endregion

    public static void showNativeLoginProviders(final Context context, final Configuration configuration, final Map<String, Object> params,
                                                final LoginProvider.LoginProviderCallbacks loginProviderCallbacks,
                                                final LoginProvider.LoginProviderTrackerCallback trackerCallback) {
        /*
        Url generation must be out of the lifecycle callback scope. Otherwise we will have a serializable error.
         */
        final String url = getPresentationUrl(configuration, params, "login");
        HostActivity.present(context, new HostActivity.HostActivityLifecycleCallbacks() {
            @Override
            public void onCreate(final AppCompatActivity activity, @Nullable Bundle savedInstanceState) {
                Bundle args = new Bundle();
                args.putString(WebViewFragment.ARG_TITLE, "Sign in");
                args.putString(WebViewFragment.ARG_URL, url);
                args.putString(WebViewFragment.ARG_REDIRECT_PREFIX, "gsapi");
                WebViewFragment.present(activity, args, new WebViewFragment.WebViewFragmentLifecycleCallbacks() {

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
                        loginProviderCallbacks.onCanceled();
                    }

                    private void login(final String provider) {
                        params.put("provider", provider);
                        LoginProvider loginProvider = LoginProviderFactory.providerFor(context, configuration,
                                provider, loginProviderCallbacks, trackerCallback);

                        if (loginProvider instanceof WebViewLoginProvider && !configuration.hasGMID()) {
                            /* WebView Provider must have basic config fields. */
                            loginProviderCallbacks.onConfigurationRequired(activity, loginProvider);
                            return;
                        }
                        if (loginProvider.clientIdRequired() && !configuration.isSynced()) {
                            loginProviderCallbacks.onConfigurationRequired(activity, loginProvider);
                            return;
                        }

                        /* Okay to release activity. */
                        activity.finish();

                        if (configuration.isSynced()) {
                            /* Update provider client id if available */
                            final String providerClientId = configuration.getAppIds().get(provider);
                            if (providerClientId != null) {
                                loginProvider.updateProviderClientId(providerClientId);
                            }
                        }

                        loginProviderCallbacks.onProviderSelected(loginProvider);
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
