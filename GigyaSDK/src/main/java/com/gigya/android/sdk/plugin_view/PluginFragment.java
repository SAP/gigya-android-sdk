package com.gigya.android.sdk.plugin_view;

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.model.account.GigyaAccount;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.ui.WebBridge;
import com.gigya.android.sdk.ui.WebViewFragment;
import com.gigya.android.sdk.ui.plugin.GigyaPluginEvent;

public class PluginFragment extends WebViewFragment {

    private IWebBridgeFactory _wbFactory;
    private boolean _obfuscate;

    public void inject(boolean obfuscate, IWebBridgeFactory wbFactory) {
        _obfuscate = obfuscate;
        _wbFactory = wbFactory;
    }

    public static void present() {

    }

    @Override
    protected void parseArguments() {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final WebBridge webBridge = _wbFactory.create(_obfuscate, new WebBridge.WebBridgeInteractions() {
            @Override
            public void onPluginEvent(GigyaPluginEvent event, String containerID) {

            }

            @Override
            public void onAuthEvent(WebBridge.AuthEvent authEvent, GigyaAccount obj) {

            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onError(GigyaError error) {

            }
        });
    }


}
