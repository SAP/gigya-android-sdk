package com.gigya.android.sdk.login.provider;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Pair;

import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.log.GigyaLogger;
import com.gigya.android.sdk.login.LoginProvider;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.model.SessionInfo;
import com.gigya.android.sdk.ui.HostActivity;
import com.gigya.android.sdk.ui.WebViewFragment;
import com.gigya.android.sdk.utils.UrlUtils;

import java.util.Map;
import java.util.TreeMap;

public class WebViewLoginProvider extends LoginProvider {

    private static final String TAG = "WebViewLoginProvider";

    private Configuration _configuration;

    public WebViewLoginProvider(Configuration configuration, LoginProviderCallbacks loginCallbacks) {
        super(loginCallbacks, null);
        _configuration = configuration;
    }

    @Override
    public String getName() {
        return "web";
    }

    @Override
    public void login(Context context, final Map<String, Object> loginParams) {
        final Pair<String, String> postRequest = getPostRequest(loginParams);
        final String provider = (String) loginParams.get("provider");
        HostActivity.present(context, new HostActivity.HostActivityLifecycleCallbacks() {
            @Override
            public void onCreate(final AppCompatActivity activity, @Nullable Bundle savedInstanceState) {
                Bundle args = new Bundle();
                args.putString(WebViewFragment.ARG_URL, postRequest.first);
                args.putString(WebViewFragment.ARG_BODY, postRequest.second);
                args.putString(WebViewFragment.ARG_REDIRECT_PREFIX, "gsapi");
                WebViewFragment.present(activity, args, new WebViewFragment.WebViewFragmentLifecycleCallbacks() {

                    @Override
                    public void onWebViewResult(Map<String, Object> result) {
                        GigyaLogger.debug(TAG, result.toString());
                        final String status = (String) result.get("status");
                        if (status != null && status.equals("ok")) {
                            final SessionInfo sessionInfo = parseSessionInfo(result);
                            loginCallbacks.onProviderSession(WebViewLoginProvider.this, sessionInfo);
                        } else {
                            loginCallbacks.onProviderLoginFailed(provider, "Failed to login");
                        }
                        activity.finish();
                    }

                    @Override
                    public void onWebViewCancel() {
                        loginCallbacks.onCanceled();
                        activity.finish();
                    }
                });
            }
        });
    }

    @NonNull
    private SessionInfo parseSessionInfo(Map<String, Object> result) {
        final String accessToken = (String) result.get("access_token");
        final String expiresInString = (String) result.get("expires_in");
        final long expiresIn = Long.parseLong(expiresInString != null ? expiresInString : "0");
        final String secret = (String) result.get("x_access_token_secret");
        return new SessionInfo(secret, accessToken, expiresIn);
    }

    @Override
    public void logout(Context context) {
        // Stub.
    }

    @Override
    public String getProviderSessionsForRequest(String tokenOrCode, long expiration, String uid) {
        // Stub.
        return null;
    }

    private Pair<String, String> getPostRequest(Map<String, Object> loginParams) {
        /* Remove if added. */
        loginParams.remove(FacebookLoginProvider.LOGIN_BEHAVIOUR);

        final String url = "https://socialize." + _configuration.getApiDomain() + "/socialize.login";
        final TreeMap<String, Object> urlParams = new TreeMap<>();
        urlParams.put("redirect_uri", "gsapi://login_result");
        urlParams.put("response_type", "token");
        urlParams.put("client_id", _configuration.getApiKey());
        urlParams.put("gmid", _configuration.getGMID());
        urlParams.put("ucid", _configuration.getUCID());

        /* x_params additions. */
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

        /* Encode post body. */
        final String encodedParams = UrlUtils.buildEncodedQuery(urlParams);
        return new Pair<>(url, encodedParams);
    }
}
