package com.gigya.android.sdk.ui.provider;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.providers.LoginProvider;
import com.gigya.android.sdk.providers.LoginProviderFactory;
import com.gigya.android.sdk.providers.provider.WebViewLoginProvider;
import com.gigya.android.sdk.ui.GigyaPresenter;
import com.gigya.android.sdk.ui.HostActivity;
import com.gigya.android.sdk.ui.WebViewFragment;
import com.gigya.android.sdk.utils.UrlUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GigyaLoginPresenter extends GigyaPresenter {

    private static final String LOG_TAG = "GigyaLoginPresenter";

    private static final String REDIRECT_URI = "gsapi://result/";

    public <T> void showNativeLoginProviders(final Context context,
                                             @LoginProvider.SocialProvider List<String> providers,
                                             final Map<String, Object> params,
                                             final GigyaLoginCallback<T> callback) {
        params.put("enabledProviders", TextUtils.join(",", providers));
        /*
        Url generation must be out of the lifecycle callback scope. Otherwise we will have a serializable error.
         */
        final String url = getPresentationUrl(_configuration, params, "login");
        HostActivity.present(context, new HostActivity.HostActivityLifecycleCallbacks() {
            @Override
            public void onCreate(final AppCompatActivity activity, @Nullable Bundle savedInstanceState) {
                Bundle args = new Bundle();
                args.putString(ProviderFragment.ARG_TITLE, "Sign in");
                args.putString(ProviderFragment.ARG_URL, url);
                args.putString(ProviderFragment.ARG_REDIRECT_PREFIX, "gsapi");
                args.putSerializable(WebViewFragment.ARG_PARAMS, (HashMap) params);
                ProviderFragment.present(activity, args, new WebViewFragment.WebViewFragmentLifecycleCallbacks() {

                    @Override
                    public void onWebViewResult(Map<String, Object> result) {
                        /* Handle result. */
                        final String provider = (String) result.get("provider");
                        if (provider == null) {
                            /* Internal check. Should not happen if SDK implementation is correct. */
                            return;
                        }

                        /* Okay to release activity. */
                        activity.finish();

                        login(context, provider, params, callback);
                    }

                    @Override
                    public void onWebViewCancel() {
                        /* User cancelled WebView. */
                        callback.onOperationCancelled();
                    }
                });
            }
        });
    }

    public void login(Context context, final String provider,
                      final Map<String, Object> params, GigyaLoginCallback callback) {
        params.put("provider", provider);
        LoginProvider loginProvider = LoginProviderFactory.providerFor(context, _configuration,
                provider, callback);

        if (loginProvider instanceof WebViewLoginProvider && !_configuration.hasGMID()) {
            /* WebView Provider must have basic config fields. */
            loginProvider.configurationRequired(context, params);
            return;
        }
        if (loginProvider.clientIdRequired() && !_configuration.isSynced()) {
            loginProvider.configurationRequired(context, params);
            return;
        }

        /* Login provider selected. */
        _accountManager.updateLoginProvider(loginProvider);
        loginProvider.trackTokenChanges(_sessionManager);

        if (_configuration.isSynced()) {
            /* Update provider client id if available */
            final String providerClientId = _configuration.getAppIds().get(provider);
            if (providerClientId != null) {
                loginProvider.updateProviderClientId(providerClientId);
            }
        }

        loginProvider.login(context, params);
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
