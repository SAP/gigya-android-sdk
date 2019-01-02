package com.gigya.android.sdk.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.utils.UrlUtils;

import java.util.HashMap;
import java.util.Map;

public class GigyaPresenter {

    private static final String REDIRECT_URI = "gsapi://result/";

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

    public static void presentNativeLogin(Context context, final Configuration configuration, final Map<String, Object> params) {
        /*
        Url generation must be out of the lifecycle callback scope. Otherwise we will have a serializable error.
         */
        final String url = getPresentationUrl(configuration, params, "login");
        HostActivity.present(context, new HostActivity.HostActivityLifecycleCallbacks() {
            @Override
            public void onCreate(AppCompatActivity activity, @Nullable Bundle savedInstanceState) {
                Bundle args = new Bundle();
                args.putString(WebViewFragment.ARG_TITLE, "Sign in");
                args.putString(WebViewFragment.ARG_URL, url);
                args.putString(WebViewFragment.ARG_REDIRECT_PREFIX, "gsapi");
                WebViewFragment.present(activity, args, new WebViewFragment.WebViewFragmentResultCallback() {

                    @Override
                    void onResult(Map<String, Object> result) {
                        /* Handle result */
                    }
                });
            }

            @Override
            public void onStart(AppCompatActivity activity) {
                // Stub.
            }

            @Override
            public void onResume(AppCompatActivity activity) {
                // Stub.
            }

            @Override
            public void onActivityResult(AppCompatActivity activity, int requestCode, int resultCode, @Nullable Intent data) {
                // Stub.
            }
        });
    }
}
