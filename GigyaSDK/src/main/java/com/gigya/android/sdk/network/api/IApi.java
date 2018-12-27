package com.gigya.android.sdk.network.api;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.network.GigyaInterceptionCallback;
import com.gigya.android.sdk.network.GigyaRequest;

import java.util.Map;

public interface IApi {

    GigyaRequest getRequest(Map<String, Object> params, GigyaCallback callback, GigyaInterceptionCallback interceptor);
}
