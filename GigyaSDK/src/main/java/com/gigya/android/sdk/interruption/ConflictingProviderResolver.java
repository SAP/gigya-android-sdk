package com.gigya.android.sdk.interruption;

import android.content.Context;

import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.network.GigyaResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConflictingProviderResolver extends LoginResolver {

    private String regToken;
    private List<String> conflictingAccounts;

    public String getRegToken() {
        return regToken;
    }

    public List<String> getConflictingAccounts() {
        return conflictingAccounts;
    }

    ConflictingProviderResolver(GigyaResponse response, List<String> conflictingAccounts, String regToken, GigyaLoginCallback loginCallback) {
        super(response, loginCallback);
        this.conflictingAccounts = conflictingAccounts;
        this.regToken = regToken;
    }

    public void resolveForSocialProvider(Context appContext, String provider, Map<String, Object> params) {
        // TODO: 24/02/2019 Will be added once supported.
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
