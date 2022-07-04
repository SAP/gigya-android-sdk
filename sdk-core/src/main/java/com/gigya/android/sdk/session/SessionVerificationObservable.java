package com.gigya.android.sdk.session;

import java.util.Observable;

public class SessionVerificationObservable extends Observable {

    public void flush() {
        deleteObservers();
    }
}
