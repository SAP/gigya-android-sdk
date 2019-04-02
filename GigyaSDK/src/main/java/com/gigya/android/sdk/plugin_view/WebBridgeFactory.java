package com.gigya.android.sdk.plugin_view;

import com.gigya.android.sdk.ui.WebBridge;

public class WebBridgeFactory implements IWebBridgeFactory {

    @Override
    public WebBridge create(boolean obfuscate, WebBridge.WebBridgeInteractions wbInteractions) {
        return new WebBridge(null, obfuscate, wbInteractions);
    }
}
