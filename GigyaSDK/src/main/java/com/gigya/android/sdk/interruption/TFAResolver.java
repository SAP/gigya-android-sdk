package com.gigya.android.sdk.interruption;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.api.tfa.TFAGetProvidersApi;
import com.gigya.android.sdk.api.tfa.TFAInitApi;
import com.gigya.android.sdk.model.tfa.TFAInitResponse;
import com.gigya.android.sdk.model.tfa.TFAProvider;
import com.gigya.android.sdk.model.tfa.TFAProvidersResponse;
import com.gigya.android.sdk.network.GigyaError;

import java.util.ArrayList;
import java.util.List;

public class TFAResolver {

    private GigyaLoginCallback loginCallback;

    private String regToken;
    private List<TFAProvider> activeProviders = new ArrayList<>();
    private List<TFAProvider> inactiveProviders = new ArrayList<>();
    private String phoneNumber;

    public TFAResolver(GigyaLoginCallback loginCallback) {
        this.loginCallback = loginCallback;
    }

    public TFAResolver regToken(String regToken) {
        this.regToken = regToken;
        return this;
    }

    public void getProviders() {
        new TFAGetProvidersApi().call(this.regToken, new GigyaCallback<TFAProvidersResponse>() {
            @Override
            public void onSuccess(TFAProvidersResponse obj) {
                loginCallback.onPendingTFARegistration(TFAResolver.this);
            }

            @Override
            public void onError(GigyaError error) {
                loginCallback.onError(error);
            }
        });
    }

    public void resolveForRegistration(String method, String phoneNumber) {
        this.phoneNumber = phoneNumber;
        String mode = "register";
        if (!activeProviders.isEmpty()) {
            for (TFAProvider provider : activeProviders) {
                if (provider.getName().equals(method)) {
                    mode = "verify";
                }
            }
        }
        new TFAInitApi().call(this.regToken, method, mode, new GigyaCallback<TFAInitResponse>() {
            @Override
            public void onSuccess(TFAInitResponse obj) {

            }

            @Override
            public void onError(GigyaError error) {
                loginCallback.onError(error);
            }
        });
    }

    public void resolveForVerification() {

    }

    private void sendVerificationCode() {

    }

    //region Getters

    public String getRegToken() {
        return regToken;
    }

    public List<TFAProvider> getActiveProviders() {
        return activeProviders;
    }

    public List<TFAProvider> getInactiveProviders() {
        return inactiveProviders;
    }

    //endregion
}
