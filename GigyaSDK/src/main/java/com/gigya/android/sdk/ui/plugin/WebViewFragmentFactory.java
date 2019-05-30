package com.gigya.android.sdk.ui.plugin;


import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.GigyaPluginCallback;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.ui.WebViewFragment;
import com.gigya.android.sdk.ui.provider.ProviderFragment;

import org.json.JSONObject;

import java.util.Map;

public class WebViewFragmentFactory<A extends GigyaAccount> implements IWebViewFragmentFactory<A> {

    private static final String REDIRECT_URL_SCHEME = "gsapi";
    private static final String ON_JS_LOAD_ERROR = "on_js_load_error";
    private static final String ON_JS_EXCEPTION = "on_js_exception";
    private static final String CONTAINER_ID = "pluginContainer";
    private static final int JS_TIMEOUT = 10000;

    final private Config _config;
    final private IGigyaWebBridge<A> _gigyaWebBridge;

    public WebViewFragmentFactory(Config config, IGigyaWebBridge<A> gigyaWebBridge) {
        _config = config;
        _gigyaWebBridge = gigyaWebBridge;
    }

    @Override
    public void showPluginFragment(AppCompatActivity activity, String plugin, Map<String, Object> params, Bundle args, GigyaPluginCallback<A> gigyaPluginCallback) {
        params.put("containerID", CONTAINER_ID);

        if (params.containsKey("commentsUI")) {
            params.put("hideShareButtons", true);
            if (params.get("version") != null && (int) params.get("version") == -1) {
                params.put("version", 2);
            }
        }

        if (params.containsKey("RatingUI") && params.get("showCommentButton") == null) {
            params.put("showCommentButton", false);
        }

        final String template =
                "<head>" +
                        "<meta name='viewport' content='initial-scale=1,maximum-scale=1,user-scalable=no' />" +
                        "<script>" +
                        "function onJSException(ex) {" +
                        "document.location.href = '%s://%s?ex=' + encodeURIComponent(ex);" +
                        "}" +
                        "function onJSLoad() {" +
                        "if (gigya && gigya.isGigya)" +
                        "window.__wasSocializeLoaded = true;" +
                        "}" +
                        "setTimeout(function() {" +
                        "if (!window.__wasSocializeLoaded)" +
                        "document.location.href = '%s://%s';" +
                        "}, %s);" +
                        "</script>" +
                        "<script src='https://cdns." + _config.getApiDomain() + "/JS/gigya.js?apikey=%s' type='text/javascript' onLoad='onJSLoad();'>" +
                        "{" +
                        "deviceType: 'mobile'" +
                        "}" +
                        "</script>" +
                        "</head>" +
                        "<body>" +
                        "<div id='%s'></div>" +
                        "<script>" +
                        "%s" +
                        "try {" +
                        "gigya._.apiAdapters.mobile.showPlugin('%s', %s);" +
                        "} catch (ex) { onJSException(ex); }" +
                        "</script>" +
                        "</body>";

        GigyaPluginFragment<A> fragment = new GigyaPluginFragment<>();
        fragment.setArguments(args);
        fragment.setConfig(_config);
        fragment.setWebBridge(_gigyaWebBridge);
        fragment.setCallback(gigyaPluginCallback);
        fragment.setHtml(String.format(template, REDIRECT_URL_SCHEME, ON_JS_EXCEPTION, REDIRECT_URL_SCHEME, ON_JS_LOAD_ERROR, JS_TIMEOUT,
                _config.getApiKey(), CONTAINER_ID, "", plugin, new JSONObject(params).toString()));
        FragmentTransaction fragmentTransaction = activity.getSupportFragmentManager().beginTransaction();
        fragmentTransaction.add(fragment, "GigyaPluginFragment");
        fragmentTransaction.commitAllowingStateLoss();
    }

    @Override
    public void showProviderFragment(final AppCompatActivity activity, final IBusinessApiService businessApiService, final Map<String, Object> params, Bundle args,
                                     final GigyaLoginCallback gigyaLoginCallback) {
        ProviderFragment.present(activity, args, new WebViewFragment.WebViewFragmentLifecycleCallbacks() {

            @Override
            public void onWebViewResult(Map<String, Object> result) {
                // Handle result.
                final String providerName = (String) result.get("provider");
                if (providerName == null) {
                    // Internal check. Should not happen if SDK implementation is correct.
                    return;
                }

                // Okay to release activity.
                activity.finish();

                businessApiService.login(providerName, params, gigyaLoginCallback);
            }

            @Override
            public void onWebViewCancel() {

                // User cancelled WebView.
                gigyaLoginCallback.onOperationCanceled();
            }
        });
    }
}
