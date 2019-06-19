package com.gigya.android.sdk.tfa.push;

import android.support.annotation.NonNull;

public interface IPushTokenFetcher {

    void getToken(@NonNull IPushTokenAvailability availabilityCallback);
}
