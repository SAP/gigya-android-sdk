package com.gigya.android.sdk.interruption;

import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.api.GigyaApiResponse;

public interface IInterruptionsHandler {

    void setEnabled(boolean enabled);

    boolean isEnabled();

    void resolve(GigyaApiResponse apiResponse, final GigyaLoginCallback loginCallback);

}
