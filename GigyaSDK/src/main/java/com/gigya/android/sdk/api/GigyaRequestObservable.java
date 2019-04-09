package com.gigya.android.sdk.api;

import com.gigya.android.sdk.network.GigyaApiRequest;

import java.util.Observable;
import java.util.Observer;

public class GigyaRequestObservable extends Observable implements IGigyaRequestObservable {

    @Override
    public synchronized void send(GigyaApiRequest gigyaApiRequest) {
        notifyObservers(gigyaApiRequest);
    }

    @Override
    public synchronized void register(Observer observer) {
        addObserver(observer);
    }
}
