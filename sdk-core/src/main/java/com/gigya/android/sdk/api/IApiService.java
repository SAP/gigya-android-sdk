package com.gigya.android.sdk.api;

public interface IApiService {

    void send(GigyaApiRequest request, boolean blocking, final ApiService.IApiServiceResponse apiCallback);

    void release();

    void cancel(String tag);
}
