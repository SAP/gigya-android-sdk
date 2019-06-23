package com.gigya.android.sdk.tfa.resolvers.push;

import android.support.annotation.NonNull;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.interruption.tfa.TFAResolver;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.adapter.RestAdapter;
import com.gigya.android.sdk.tfa.GigyaDefinitions;
import com.gigya.android.sdk.tfa.models.InitTFAModel;

import java.util.HashMap;
import java.util.Map;

public class PushVerificationResolver<A extends GigyaAccount> extends TFAResolver<A> implements IPushVerificationResolver {

    public PushVerificationResolver(GigyaLoginCallback<A> loginCallback,
                                    GigyaApiResponse interruption,
                                    IBusinessApiService<A> businessApiService) {
        super(loginCallback, interruption, businessApiService);
    }

    public void sendVerificaion(@NonNull final ResultCallback resultCallback) {
        // Send init tfa.
        final Map<String, Object> params = new HashMap<>();
        params.put("regToken", getRegToken());
        params.put("provider", GigyaDefinitions.TFAProvider.PUSH);
        params.put("mode", "verify");
        _businessApiService.send(GigyaDefinitions.API.API_TFA_INIT, params, RestAdapter.POST,
                InitTFAModel.class, new GigyaCallback<InitTFAModel>() {
                    @Override
                    public void onSuccess(InitTFAModel model) {
                        _gigyaAssertion = model.getGigyaAssertion();

                        // Send verification.
                        params.clear();
                        params.put("regToken", getRegToken());
                        params.put("gigyaAssertion", _gigyaAssertion);
                        //_businessApiService.send(GigyaDefinitions.API.API_TFA_SEND_VERIFICATION, params, RestAdapter.POST,null, null);

                    }

                    @Override
                    public void onError(GigyaError error) {
                        resultCallback.onError(error);
                    }
                });
    }

    @Override
    public void verify(@NonNull final ResultCallback resultCallback) {
        _businessApiService.getAccount(new GigyaCallback<A>() {
            @Override
            public void onSuccess(A account) {
                _loginCallback.onSuccess(account);
                resultCallback.onResolved();
            }

            @Override
            public void onError(GigyaError error) {
                resultCallback.onError(error);
            }
        });
    }

    public interface ResultCallback {

        void onResolved();

        void onError(GigyaError error);
    }
}
