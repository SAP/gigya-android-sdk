package com.gigya.android.sdk.providers.external;

import android.content.Context;

import java.util.Map;

public interface IProviderWrapper {

    void login(Context context, final Map<String, Object> params, final IProviderWrapperCallback callback);

    void logout();
}
