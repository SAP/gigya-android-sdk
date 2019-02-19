package com.gigya.android.sdk.interruption;

import com.gigya.android.sdk.GigyaLoginCallback;

import java.util.HashMap;
import java.util.Map;

public class LinkedAccountResolver extends LoginResolver {

    public LinkedAccountResolver(GigyaLoginCallback loginCallback) {
        super(loginCallback);
    }

    public void resolve(String regToken) {
        Map<String, Object> params = new HashMap<>();
        params.put("regToken", regToken);
        apiManager.finalizeRegistration(params, loginCallback);
    }
}
