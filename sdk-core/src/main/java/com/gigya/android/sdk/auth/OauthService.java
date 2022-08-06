package com.gigya.android.sdk.auth;

import com.gigya.android.sdk.api.ApiService;
import com.gigya.android.sdk.api.GigyaApiRequest;
import com.gigya.android.sdk.api.IApiRequestFactory;
import com.gigya.android.sdk.api.IApiService;
import com.gigya.android.sdk.network.adapter.RestAdapter;

import java.util.HashMap;
import java.util.Map;

public class OauthService implements IOauthService {

    final private IApiService apiService;
    final private IApiRequestFactory requestFactory;

    public OauthService(
            IApiService apiService,
            IApiRequestFactory requestFactory
    ) {
        this.apiService = apiService;
        this.requestFactory = requestFactory;
    }

    private enum OauthApis {
        connect("oauth.connect"),
        authorize("oauth.authorize"),
        token("oauth.token");

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
    public void connect(String token, final ApiService.IApiServiceResponse iApiServiceResponse) {
        final HashMap<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + token);
        GigyaApiRequest request = this.requestFactory.create(
                OauthApis.connect.api, null, RestAdapter.HttpMethod.POST, headers
        );
        this.apiService.send(request, iApiServiceResponse);
    }

    /**
     * @param token
     */
    @Override
    public void authorize(String token, final ApiService.IApiServiceResponse iApiServiceResponse) {
        final Map<String, Object> params = new HashMap<>();
        params.put("response_type", "code");
        final HashMap<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + token);
        GigyaApiRequest request = this.requestFactory.create(
                OauthApis.authorize.api, params, RestAdapter.HttpMethod.POST, headers
        );
        this.apiService.send(request, iApiServiceResponse);
    }

    /**
     * @param code
     */
    @Override
    public void token(String code, final ApiService.IApiServiceResponse iApiServiceResponse) {
        final Map<String, Object> params = new HashMap<>();
        params.put("grant_type", "authorization_code");
        params.put("code", code);
        GigyaApiRequest request = this.requestFactory.create(
                OauthApis.token.api, params, RestAdapter.HttpMethod.POST
        );
        this.apiService.send(request, iApiServiceResponse);
    }

}


