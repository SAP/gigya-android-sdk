package com.gigya.android.sdk.site;

enum NoLoginBehavior {
    tempUser,
    alwaysLogin,
    loginExistingUser,
}

public class GigyaPlugins {
    public NoLoginBehavior connectWithoutLoginBehavior = NoLoginBehavior.alwaysLogin;
    public String defaultRegScreenSet;
    public String defaultMobileRegScreenSet;
    public int sessionExpiration;
    public int rememberSessionExpiration;
}