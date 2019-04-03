package com.gigya.android.sdk.plugin_view;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.gigya.android.sdk.GigyaPluginCallback;

public class PluginFragmentFactory implements IPluginFragmentFactory {

    final private IWebBridgeFactory _wbFactory;

    PluginFragmentFactory(IWebBridgeFactory wbFactory) {
        _wbFactory = wbFactory;
    }

    @Override
    public void showFragment(AppCompatActivity activity, Bundle args, GigyaPluginCallback gigyaPluginCallback) {
        PluginFragment.present(activity, args, _wbFactory, gigyaPluginCallback);
    }
}
