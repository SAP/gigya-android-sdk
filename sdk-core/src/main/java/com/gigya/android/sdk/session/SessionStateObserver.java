package com.gigya.android.sdk.session;

import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

public abstract class SessionStateObserver implements Observer {

    public abstract void onSessionInvalidated(Object o);

    @Override
    public void update(Observable observable, Object o) {
        onSessionInvalidated(o);
    }
}
