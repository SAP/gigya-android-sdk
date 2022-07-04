package com.gigya.android.sdk.session;

import java.util.Observable;

public class SessionExpirationObservable extends Observable {

    public void flush() {
        deleteObservers();
    }

}
