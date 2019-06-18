package com.gigya.android.sdk.tfa.api;

import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.session.ISessionService;

public class TFABusinessApiService implements ITFABusinessApiService {

    final private ISessionService _sessionService;
    final private IBusinessApiService _businessApiService;

    public TFABusinessApiService(ISessionService sessionService,
                                 IBusinessApiService businessApiService) {
        _sessionService = sessionService;
        _businessApiService = businessApiService;
    }

    public void optIntoPush() {
        if (!_sessionService.isValid()) {
            return;
        }
    }

    public void updateDeviceInfo() {

    }
}
