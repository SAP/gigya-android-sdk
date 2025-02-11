package com.gigya.android.sdk.ui.plugin;


import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.GigyaPluginCallback;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.ui.Presenter;
import com.gigya.android.sdk.ui.WebViewFragment;
import com.gigya.android.sdk.ui.provider.ProviderFragment;

import org.json.JSONObject;

import java.util.Map;

public class WebViewFragmentFactory<A extends GigyaAccount> implements IWebViewFragmentFactory<A> {

    final private Config _config;
    final private IGigyaWebBridge<A> _gigyaWebBridge;

    public WebViewFragmentFactory(Config config, IGigyaWebBridge<A> gigyaWebBridge) {
        _config = config;
        _gigyaWebBridge = gigyaWebBridge;
    }

    @Override
    public void showPluginFragment(AppCompatActivity activity,
                                   String plugin, Map<String, Object> params,
                                   Bundle args,
                                   GigyaPluginCallback<A> gigyaPluginCallback) {
        params.put("containerID", Presenter.Consts.CONTAINER_ID);

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
                        "<link rel=\"icon\" href=\"data:,\">" +
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
                        "<script src='https://" +
                        (_config.isCnameEnabled() ? _config.getCname() : "cdns." + _config.getApiDomain()) +
                        "/JS/gigya.js?apikey=%s&lang=%s' type='text/javascript' onLoad='onJSLoad();'>" +
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

        final GigyaPluginFragment<A> fragment = new GigyaPluginFragment<>();
        fragment.setArguments(args);
        fragment.setConfig(_config);
        fragment.setWebBridge(_gigyaWebBridge);
        fragment.setCallback(gigyaPluginCallback);
        fragment.setHtml(String.format(template,
                Presenter.Consts.REDIRECT_URL_SCHEME,
                Presenter.Consts.ON_JS_EXCEPTION,
                Presenter.Consts.REDIRECT_URL_SCHEME,
                Presenter.Consts.ON_JS_LOAD_ERROR,
                Presenter.Consts.JS_TIMEOUT,
                _config.getApiKey(),
                params.get("lang"),
                Presenter.Consts.CONTAINER_ID,
                "", // js script before showing the plugin
                plugin,
                new JSONObject(params).toString()));
        FragmentTransaction fragmentTransaction = activity.getSupportFragmentManager().beginTransaction();
        fragmentTransaction.add(fragment, "GigyaPluginFragment");
        fragmentTransaction.commitAllowingStateLoss();
    }

    @Override
    public void showProviderFragment(final AppCompatActivity activity,
                                     final Config config,
                                     final IBusinessApiService businessApiService,
                                     final Map<String, Object> params, Bundle args,
                                     final GigyaLoginCallback gigyaLoginCallback) {
        ProviderFragment.present(activity, _config, args, new WebViewFragment.WebViewFragmentLifecycleCallbacks() {

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
