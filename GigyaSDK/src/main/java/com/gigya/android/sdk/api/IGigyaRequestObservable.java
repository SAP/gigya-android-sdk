package com.gigya.android.sdk.api;

import com.gigya.android.sdk.network.GigyaApiRequest;

import java.util.Observer;

public interface IGigyaRequestObservable {

    void send(GigyaApiRequest gigyaApiRequest);

    void register(Observer observer);
}
