package com.gigya.android.sdk.tfa.resolvers.push;

import android.support.annotation.NonNull;

public interface IPushVerificationResolver {

    void verify(@NonNull PushVerificationResolver.ResultCallback resultCallback);
}
