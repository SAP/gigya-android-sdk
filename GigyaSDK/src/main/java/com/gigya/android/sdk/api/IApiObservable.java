package com.gigya.android.sdk.api;

import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.providers.IProviderPermissionsCallback;

import java.util.Map;
import java.util.Observer;

public interface IApiObservable<A> {

    void send(String api, Map<String, Object> params, GigyaLoginCallback<A> callback);

    void send(String api, Map<String, Object> params, GigyaLoginCallback<A> callback, Runnable completionHandler);

    void send(String api , Map<String, Object> params, IProviderPermissionsCallback permissionsCallback);

    ApiObservable register(Observer observer);

    void dispose();
}
