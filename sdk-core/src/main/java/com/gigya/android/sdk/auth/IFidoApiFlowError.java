package com.gigya.android.sdk.auth;

import com.gigya.android.sdk.network.GigyaError;

public interface IFidoApiFlowError {

    void onFlowFailedWith(GigyaError error);
}
