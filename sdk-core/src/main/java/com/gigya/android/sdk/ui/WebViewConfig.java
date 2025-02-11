package com.gigya.android.sdk.ui;

public class WebViewConfig {

    boolean localStorage = true;
    boolean allowFileAccess = true;
    boolean javaScriptEnabled = true;

    public boolean isLocalStorage() {
        return localStorage;
    }

    public void setLocalStorage(boolean localStorage) {
        this.localStorage = localStorage;
    }


    public boolean isAllowFileAccess() {
        return allowFileAccess;
    }

    public void setAllowFileAccess(boolean allowFileAccess) {
        this.allowFileAccess = allowFileAccess;
    }

    public boolean isJavaScriptEnabled() {
        return javaScriptEnabled;
    }

    public void setJavaScriptEnabled(boolean javaScriptEnabled) {
        this.javaScriptEnabled = javaScriptEnabled;
    }
}
