package com.gigya.android.sdk.interruption;

import android.content.Context;

import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.ui.provider.GigyaLoginPresenter;

import java.util.HashMap;
import java.util.Map;

public class ConflictingProviderResolver extends LoginResolver {

    private String regToken;

    public String getRegToken() {
        return regToken;
    }

    ConflictingProviderResolver(GigyaLoginCallback loginCallback, String regToken) {
        super(loginCallback);
        this.regToken = regToken;
    }

    public void resolveForSocialProvider(Context appContext, String provider, Map<String, Object> params) {
        new GigyaLoginPresenter().login(appContext, provider, params, loginCallback);
    }

    public void resolveForSiteProvider(String loginId, String password) {
        Map<String, Object> params = new HashMap<>();
        params.put("loginID", loginId);
        params.put("password", password);
        params.put("loginMode", "link");
        params.put("regToken", regToken);
        apiManager.login(params, loginCallback);
    }
}
