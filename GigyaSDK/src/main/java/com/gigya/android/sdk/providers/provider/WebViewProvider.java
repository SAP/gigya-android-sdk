package com.gigya.android.sdk.providers.provider;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Pair;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.account.IAccountService;
import com.gigya.android.sdk.api.IApiService;
import com.gigya.android.sdk.model.account.SessionInfo;
import com.gigya.android.sdk.persistence.IPersistenceService;
import com.gigya.android.sdk.session.ISessionService;
import com.gigya.android.sdk.ui.HostActivity;
import com.gigya.android.sdk.ui.WebViewFragment;
import com.gigya.android.sdk.ui.provider.ProviderFragment;
import com.gigya.android.sdk.utils.UrlUtils;

import java.util.Map;
import java.util.TreeMap;

public class WebViewProvider extends Provider {

    private static final String LOG_TAG = "WebViewProvider";

    public WebViewProvider(Config config, ISessionService sessionService, IAccountService accountService,
                           IApiService apiService, IPersistenceService persistenceService, GigyaLoginCallback gigyaLoginCallback) {
        super(config, sessionService, accountService, apiService, persistenceService, gigyaLoginCallback);
    }

    @Override
    public String getName() {
        return "web";
    }

    @Override
    public void login(Context context, Map<String, Object> loginParams, String loginMode) {
        _loginMode = loginMode;
        final Pair<String, String> postRequest = getPostRequest(loginParams);
        HostActivity.present(context, new HostActivity.HostActivityLifecycleCallbacks() {
            @Override
            public void onCreate(final AppCompatActivity activity, @Nullable Bundle savedInstanceState) {
                Bundle args = new Bundle();
                args.putString(ProviderFragment.ARG_URL, postRequest.first);
                args.putString(ProviderFragment.ARG_BODY, postRequest.second);
                args.putString(ProviderFragment.ARG_REDIRECT_PREFIX, "gsapi");
                ProviderFragment.present(activity, args, new WebViewFragment.WebViewFragmentLifecycleCallbacks() {

                    @Override
                    public void onWebViewResult(Map<String, Object> result) {
                        GigyaLogger.debug(LOG_TAG, result.toString());
                        final String status = (String) result.get("status");
                        if (status != null && status.equals("ok")) {
                            final SessionInfo sessionInfo = parseSessionInfo(result);
                            onProviderSession(sessionInfo);
                        } else {
                            onLoginFailed("Failed to login");
                        }
                        activity.finish();
                    }

                    @Override
                    public void onWebViewCancel() {
                        onCanceled();
                        activity.finish();
                    }
                });
            }
        });
    }

    @Override
    public void logout(Context context) {
        // Stub.
    }

    @Override
    public String getProviderSessions(String tokenOrCode, long expiration, String uid) {
        return null;
    }

    @Override
    public boolean supportsTokenTracking() {
        return false;
    }

    @Override
    public void trackTokenChange() {
        // Stub.
    }

    private SessionInfo parseSessionInfo(Map<String, Object> result) {
        final String accessToken = (String) result.get("access_token");
        final String expiresInString = (String) result.get("expires_in");
        final long expiresIn = Long.parseLong(expiresInString != null ? expiresInString : "0");
        final String secret = (String) result.get("x_access_token_secret");
        return new SessionInfo(secret, accessToken, expiresIn);
    }

    private Pair<String, String> getPostRequest(Map<String, Object> loginParams) {
        /* Remove if added. */
        loginParams.remove(FacebookProvider.LOGIN_BEHAVIOUR);

        final String url = "https://socialize." + _config.getApiDomain() + "/socialize.login";
        final TreeMap<String, Object> urlParams = new TreeMap<>();
        urlParams.put("redirect_uri", "gsapi://login_result");
        urlParams.put("response_type", "token");
        urlParams.put("client_id", _config.getApiKey());
        urlParams.put("gmid", _config.getGmid());
        urlParams.put("ucid", _config.getUcid());

        // x_params additions.
        for (Map.Entry entry : loginParams.entrySet()) {
            final String key = (String) entry.getKey();
            Object value = loginParams.get(key);
            if (value != null) {
                if (key.startsWith("x_"))
                    urlParams.put(key, value);
                else
                    urlParams.put("x_" + key, value);
            }
        }
        urlParams.put("x_secret_type", "oauth1");
        urlParams.put("x_endPoint", "socialize.login");
        urlParams.put("x_sdk", Gigya.VERSION);
        final String provider = (String) loginParams.get("provider");
        if (provider != null) {
            urlParams.put("x_provider", provider);
        }

        // Encode post body.
        final String encodedParams = UrlUtils.buildEncodedQuery(urlParams);
        return new Pair<>(url, encodedParams);
    }
}
