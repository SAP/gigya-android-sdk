package com.gigya.android.sdk.interruption;

import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.api.GigyaApiRequest;
import com.gigya.android.sdk.api.GigyaApiResponse;

public interface IInterruptionResolverFactory {

    void setEnabled(boolean enabled);

    boolean isEnabled();

    void resolve(GigyaApiResponse apiResponse, final GigyaLoginCallback loginCallback);

    void resolve(GigyaApiResponse apiResponse, final GigyaLoginCallback loginCallback, final GigyaApiRequest originalRequest);

}
