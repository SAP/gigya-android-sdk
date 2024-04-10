package com.gigya.android.sdk.auth;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.api.IBusinessApiService;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("ALL")
public class OauthService implements IOauthService {

    final private IBusinessApiService businessApiService;

    public Map<String, Object> loginParameters;

    public OauthService(
            IBusinessApiService businessApiService
    ) {
        this.businessApiService = businessApiService;
    }

    private enum OauthApis {
        connect("oauth.connect"),
        authorize("oauth.authorize"),
        token("oauth.token"),
        disconnect("oauth.disconnect");

        private final String api;

        OauthApis(String api) {
            this.api = api;
        }

        public String api() {
            return this.api;
        }
    }

    /**
     * @param token
     */
    @Override
    public void connect(String token, final GigyaCallback<GigyaApiResponse> callback) {
        final HashMap<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + token);
        this.businessApiService.send(
                OauthApis.connect.api,
                new HashMap<String, Object>(),
                headers,
                GigyaApiResponse.class,
                callback
        );
    }

    @Override
    public void disconnect(String regToken, String idToken, boolean ignoreApiQueue, final GigyaCallback<GigyaApiResponse> callback) {
        final HashMap<String, Object> params = new HashMap<>();
        params.put("ignoreApiQueue", ignoreApiQueue);
        params.put("regToken", regToken);
        final HashMap<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + idToken);
        this.businessApiService.send(
                OauthApis.disconnect.api,
                params,
                headers,
                GigyaApiResponse.class,
                callback
        );
    }

    /**
     * @param token
     */
    @Override
    public void authorize(String token, GigyaCallback<GigyaApiResponse> callback) {
        final Map<String, Object> params = new HashMap<>();
        params.put("response_type", "code");
        final HashMap<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + token);
        this.businessApiService.send(
                OauthApis.authorize.api,
                params,
                headers,
                GigyaApiResponse.class,
                callback
        );
    }

    /**
     * @param code
     */
    @Override
    public void token(String code, GigyaCallback<GigyaApiResponse> callback) {
        final Map<String, Object> params = new HashMap<>();
        params.put("grant_type", "authorization_code");
        params.put("code", code);
        // Merge with login parameters if relevant.
        if (loginParameters != null) {
            params.putAll(loginParameters);
        }
        this.businessApiService.send(
                OauthApis.token.api,
                params,
                new HashMap<String, String>(),
                GigyaApiResponse.class,
                callback
        );
    }

    @Override
    public void setLoginParams(Map<String, Object> params) {
        if (loginParameters == null) {
            loginParameters = new HashMap<>();
        }
        loginParameters.putAll(params);
    }

    @Override
    public void clearLoginParams() {
        if (loginParameters != null) {
            loginParameters.clear();
        }
    }

}


