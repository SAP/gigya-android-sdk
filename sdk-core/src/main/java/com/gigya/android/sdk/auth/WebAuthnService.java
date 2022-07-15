package com.gigya.android.sdk.auth;


import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.api.ApiService;
import com.gigya.android.sdk.api.GigyaApiRequest;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.api.IApiRequestFactory;
import com.gigya.android.sdk.api.IApiService;
import com.gigya.android.sdk.auth.models.WebAuthnInitRegisterResponseModel;
import com.gigya.android.sdk.network.GigyaError;

import java.util.HashMap;
import java.util.Map;

public class WebAuthnService implements IWebAuthnService {

    public static final String LOG_TAG = "WebAuthnService";

    final private IApiService apiService;
    final private IApiRequestFactory requestFactory;
    final private IOauthService oauthService;

    public WebAuthnService(
            IOauthService oauthService,
            IApiService apiService,
            IApiRequestFactory requestFactory
    ) {
        this.oauthService = oauthService;
        this.apiService = apiService;
        this.requestFactory = requestFactory;
    }

    private enum WebAuthnApis {
        initRegisterCredentials("accounts.auth.fido.initRegisterCredentials"),
        getAssertionOptions("accounts.auth.fido.getAssertionOptions"),
        registerCredentials("accounts.auth.fido.registerCredentials"),
        verifyAssertion("accounts.auth.fido.verifyAssertion");

        final String api;

        WebAuthnApis(String api) {
            this.api = api;
        }

        public String api() {
            return this.api;
        }
    }

    /**
     *
     */
    @Override
    public void initRegistration() {
        GigyaApiRequest request = requestFactory.create(
                WebAuthnApis.initRegisterCredentials.api,
                new HashMap<String, Object>());
        apiService.send(request, new ApiService.IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                GigyaLogger.debug(LOG_TAG, "initRegistration success:\n" + response.asJson());

                WebAuthnInitRegisterResponseModel webAuthnInitRegisterResponseModel =
                        response.parseTo(WebAuthnInitRegisterResponseModel.class);

                if (webAuthnInitRegisterResponseModel == null) {
                    GigyaLogger.debug(LOG_TAG, "initRegistration webAuthnInitRegisterResponseModel parse error");
                    return;
                }


            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                GigyaLogger.debug(LOG_TAG, "initRegistration error:\n" + gigyaError.getData());
            }
        });
    }

    /**
     * @param params
     */
    @Override
    public void registerCredentials(Map<String, Object> params) {
        GigyaApiRequest request = requestFactory.create(
                WebAuthnApis.registerCredentials.api,
                params);
        apiService.send(request, new ApiService.IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                GigyaLogger.debug(LOG_TAG, "registerCredentials success:\n" + response.asJson());

            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                GigyaLogger.debug(LOG_TAG, "registerCredentials error:\n" + gigyaError.getData());
            }
        });
    }

    /**
     *
     */
    @Override
    public void getAssertionOptions() {
        GigyaApiRequest request = requestFactory.create(
                WebAuthnApis.getAssertionOptions.api,
                new HashMap<String, Object>());
        apiService.send(request, new ApiService.IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                GigyaLogger.debug(LOG_TAG, "getAssertionOptions success:\n" + response.asJson());

            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                GigyaLogger.debug(LOG_TAG, "getAssertionOptions error:\n" + gigyaError.getData());
            }
        });
    }

    /**
     * @param params
     */
    @Override
    public void verifyAssertion(Map<String, Object> params) {
        GigyaApiRequest request = requestFactory.create(
                WebAuthnApis.verifyAssertion.api,
                params);
        apiService.send(request, new ApiService.IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                GigyaLogger.debug(LOG_TAG, "verifyAssertion success:\n" + response.asJson());

            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                GigyaLogger.debug(LOG_TAG, "verifyAssertion error:\n" + gigyaError.getData());
            }
        });
    }


}
