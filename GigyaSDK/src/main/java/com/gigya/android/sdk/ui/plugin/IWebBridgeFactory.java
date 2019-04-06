package com.gigya.android.sdk.ui.plugin;

public interface IWebBridgeFactory {

    WebBridge create(boolean obfuscate, IWebBridge wbInteractions);
}
