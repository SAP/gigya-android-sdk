package com.gigya.android.sdk.api.interruption;

import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.network.GigyaApiResponse;

public interface IInterruptionsResolver {

    void setEnabled(boolean enabled);

    void clearAll();

    boolean evaluateInterruptionError(GigyaApiResponse apiResponse, final GigyaLoginCallback loginCallback);

    boolean evaluateInterruptionSuccess(GigyaApiResponse apiResponse);

}
