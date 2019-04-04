package com.gigya.android.sdk.plugin_view;

import com.gigya.android.sdk.ui.IWebBridge;
import com.gigya.android.sdk.ui.WebBridge;

public interface IWebBridgeFactory {

    WebBridge create(boolean obfuscate, IWebBridge wbInteractions);
}
