package com.gigya.android.sdk;

public abstract class GigyaInterceptor {

    private final String name;

    protected GigyaInterceptor(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract void intercept();
}
