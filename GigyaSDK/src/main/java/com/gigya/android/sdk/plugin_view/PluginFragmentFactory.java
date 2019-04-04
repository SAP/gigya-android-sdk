package com.gigya.android.sdk.plugin_view;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.gigya.android.sdk.GigyaPluginCallback;
import com.gigya.android.sdk.services.Config;

public class PluginFragmentFactory implements IPluginFragmentFactory {

    final private IWebBridgeFactory _wbFactory;
    final private Config _config;

    PluginFragmentFactory(Config config, IWebBridgeFactory wbFactory) {
        _config = config;
        _wbFactory = wbFactory;
    }

    @Override
    public void showFragment(AppCompatActivity activity, Bundle args, GigyaPluginCallback gigyaPluginCallback) {
        PluginFragment.present(activity, args, _config, _wbFactory, gigyaPluginCallback);
    }
}
