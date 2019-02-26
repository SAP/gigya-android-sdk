package com.gigya.android.sdk.interruption.tfa;

import android.support.annotation.Nullable;

import com.gigya.android.sdk.AccountManager;
import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.api.account.FinalizeRegistrationApi;
import com.gigya.android.sdk.api.tfa.TFAFinalizeApi;
import com.gigya.android.sdk.api.tfa.TFAGetProvidersApi;
import com.gigya.android.sdk.api.tfa.phone.TFACompleteVerificationApi;
import com.gigya.android.sdk.interruption.GigyaResolver;
import com.gigya.android.sdk.model.GigyaAccount;
import com.gigya.android.sdk.model.tfa.TFACompleteVerificationResponse;
import com.gigya.android.sdk.model.tfa.TFAProvider;
import com.gigya.android.sdk.model.tfa.TFAProvidersResponse;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.GigyaResponse;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class TFAResolver<T extends GigyaAccount> extends GigyaResolver {

    final private WeakReference<GigyaLoginCallback<T>> loginCallback;
    private String regToken;

    private TFAPhoneResolver<T> phoneResolver;
    private TFATotpResolver<T> totpResolver;

    public TFAResolver(NetworkAdapter networkAdapter, SessionManager sessionManager, AccountManager accountManager,
                       GigyaLoginCallback<T> loginCallback) {
        super(networkAdapter, sessionManager, accountManager);
        this.loginCallback = new WeakReference<>(loginCallback);
    }

    public void setRegToken(String regToken) {
        this.regToken = regToken;
    }

    public void init() {
        new TFAGetProvidersApi(networkAdapter, sessionManager)
                .call(this.regToken, new GigyaCallback<TFAProvidersResponse>() {
                    @Override
                    public void onSuccess(TFAProvidersResponse obj) {
                        evaluateProviders(obj);
                    }

                    @Override
                    public void onError(GigyaError error) {
                        if (loginCallback.get() != null)
                            loginCallback.get().onError(error);
                    }
                });
    }

    private void evaluateProviders(TFAProvidersResponse res) {
        if (res.getActiveProviders().isEmpty()) {
            initResolvers(res.getInactiveProviders());
            if (loginCallback.get() != null)
                loginCallback.get().onPendingTFARegistration(this);
        } else {
            initResolvers(res.getActiveProviders());
            if (loginCallback.get() != null)
                loginCallback.get().onPendingTFAVerification(this);
        }
    }

    private void initResolvers(List<TFAProvider> providers) {
        for (TFAProvider provider : providers) {
            switch (provider.getName()) {
                case "gigyaPhone":
                    phoneResolver = new TFAPhoneResolver<>(networkAdapter, sessionManager, accountManager, regToken, loginCallback);
                    break;
                case "gigyaTotp":
                    totpResolver = new TFATotpResolver<>(networkAdapter, sessionManager, accountManager, regToken, loginCallback);
                    break;
            }
        }
    }

    @Nullable
    public TFAPhoneResolver getPhoneResolver() {
        return phoneResolver;
    }

    @Nullable
    public TFATotpResolver getTotpResolver() {
        return totpResolver;
    }

    public ArrayList<String> getProviders() {
        ArrayList<String> providers = new ArrayList<>(2);
        if (phoneResolver != null) {
            providers.add(GigyaResolver.TFA_PHONE);
        }
        if (totpResolver != null) {
            providers.add(GigyaResolver.TFA_TOTP);
        }
        return providers;
    }

    @Nullable
    private String getAssertion(String selectedProvider) {
        if (selectedProvider.equals(GigyaResolver.TFA_PHONE)) {
            return phoneResolver.getGigyaAssertion();
        }
        return null;
    }

    @Nullable
    private String getPhvToken(String selectedProvider) {
        if (selectedProvider.equals(GigyaResolver.TFA_PHONE)) {
            return phoneResolver.getPhvToken();
        }
        return null;
    }

    public void complete(final String selectedProvider, String code) {
        new TFACompleteVerificationApi(networkAdapter, sessionManager)
                .call(getAssertion(selectedProvider), getPhvToken(selectedProvider), code, new GigyaCallback<TFACompleteVerificationResponse>() {
                    @Override
                    public void onSuccess(TFACompleteVerificationResponse obj) {
                        finalizeTFA(selectedProvider, obj.getProviderAssertion());
                    }

                    @Override
                    public void onError(GigyaError error) {
                        if (loginCallback.get() != null) {
                            loginCallback.get().onError(error);
                        }
                    }
                });
    }

    private void finalizeTFA(String selectedProvider, String providerAssertion) {
        new TFAFinalizeApi(networkAdapter, sessionManager)
                .call(getAssertion(selectedProvider), providerAssertion, regToken, new GigyaCallback<GigyaResponse>() {
                    @Override
                    public void onSuccess(GigyaResponse obj) {
                        finalizeRegistration();
                    }

                    @Override
                    public void onError(GigyaError error) {
                        if (loginCallback.get() != null) {
                            loginCallback.get().onError(error);
                        }
                    }
                });
    }

    private void finalizeRegistration() {
        if (loginCallback.get() != null) {
            new FinalizeRegistrationApi<T>(networkAdapter, sessionManager, accountManager)
                    .call(regToken, loginCallback.get());
        }
    }
}
