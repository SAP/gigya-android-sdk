package com.gigya.android.sdk.interruption;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.session.ISessionService;

import java.util.HashMap;
import java.util.Map;

public class GigyaFinalizeResolver<A extends GigyaAccount> extends GigyaResolver<A> {

    private static final String LOG_TAG = "GigyaFinalizeResolver";

    public GigyaFinalizeResolver(Config config,
                                 ISessionService sessionService,
                                 IBusinessApiService<A> businessApiService,
                                 GigyaApiResponse originalResponse,
                                 GigyaLoginCallback<A> loginCallback) {
        super(config, sessionService, businessApiService, originalResponse, loginCallback);
    }

    protected void finalizeRegistration() {
        if (isAttached()) {
            GigyaLogger.debug(LOG_TAG, "finalizeRegistration: ");
            // Params.
            final Map<String, Object> params = new HashMap<>();
            params.put("regToken", _regToken);
            params.put("include", "profile,data,emails,subscriptions,preferences");
            params.put("includeUserInfo", "true");
            _businessApiService.finalizeRegistration(params, _loginCallback.get());
        }
    }
}
