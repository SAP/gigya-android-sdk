package com.gigya.android.sdk.tfa.api;

import android.support.annotation.NonNull;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.adapter.RestAdapter;
import com.gigya.android.sdk.session.ISessionService;
import com.gigya.android.sdk.tfa.GigyaDefinitions;
import com.gigya.android.sdk.tfa.models.InitTFAModel;

import java.util.HashMap;
import java.util.Map;

public class TFABusinessApiService implements ITFABusinessApiService {

    final private ISessionService _sessionService;
    final private IBusinessApiService _businessApiService;

    public TFABusinessApiService(ISessionService sessionService,
                                 IBusinessApiService businessApiService) {
        _sessionService = sessionService;
        _businessApiService = businessApiService;
    }

    @Override
    public void optIntoPush(final String deviceInfo, @NonNull final GigyaCallback<GigyaApiResponse> gigyaCallback) {
        if (!_sessionService.isValid()) {
            return;
        }

        // Initialize TFA first.
        final Map<String, Object> params = new HashMap<>();
        params.put("provider", GigyaDefinitions.TFAProvider.PUSH);
        params.put("mode", "register");
        _businessApiService.send(GigyaDefinitions.API.API_TFA_INIT, params, RestAdapter.POST,
                InitTFAModel.class, new GigyaCallback<InitTFAModel>() {

                    @Override
                    public void onSuccess(InitTFAModel model) {
                        final String gigyaAssertion = model.getGigyaAssertion();
                        params.clear();
                        params.put("gigyaAssertion", gigyaAssertion);
                        params.put("deviceInfo", deviceInfo);
                        _businessApiService.send(GigyaDefinitions.API.API_TFA_PUSH_OPT_IN, params, RestAdapter.POST,
                                GigyaApiResponse.class, gigyaCallback);
                    }

                    @Override
                    public void onError(GigyaError error) {
                        gigyaCallback.onError(error);
                    }
                });
    }

    public void updateDeviceInfo() {

    }
}
