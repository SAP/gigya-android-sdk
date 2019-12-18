package com.gigya.android.gigyademo.model;

import com.gigya.android.sdk.network.GigyaError;

public class ErrorEvent {

    private GigyaError error;
    private boolean observed = false;

    public ErrorEvent(GigyaError error) {
        this.error = error;
    }

    public GigyaError getError() {
        return error;
    }

    public boolean isObserved() {
        return observed;
    }

    public void setObserved(boolean observed) {
        this.observed = observed;
    }
}
