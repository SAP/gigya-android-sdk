package com.gigya.android.sdk.ui.plugin;


import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.GigyaPluginCallback;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.providers.IProviderFactory;
import com.gigya.android.sdk.ui.WebViewFragment;
import com.gigya.android.sdk.ui.provider.ProviderFragment;

import java.util.Map;

public class WebViewFragmentFactory implements IWebViewFragmentFactory {

    final private Context _context;
    final private IWebBridgeFactory _wbFactory;
    final private IProviderFactory _providerFactory;
    final private Config _config;

    public WebViewFragmentFactory(Context context, Config config, IWebBridgeFactory wbFactory, IProviderFactory providerFactory) {
        _context = context;
        _config = config;
        _wbFactory = wbFactory;
        _providerFactory = providerFactory;
    }

    @Override
    public void showPluginFragment(AppCompatActivity activity, Bundle args, GigyaPluginCallback gigyaPluginCallback) {
        PluginFragment.present(activity, args, _config, _wbFactory, gigyaPluginCallback);
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

                businessApiService.login(_context, providerName, params, gigyaLoginCallback);
            }

            @Override
            public void onWebViewCancel() {

                // User cancelled WebView.
                gigyaLoginCallback.onOperationCanceled();
            }
        });
    }
}
