package com.gigya.android.sdk.model.account;

import android.support.annotation.NonNull;

import com.gigya.android.sdk.gson.IPostGsonProcessable;
import com.google.gson.annotations.SerializedName;

public class SessionInfo implements IPostGsonProcessable {

    private String sessionToken;
    private String sessionSecret;
    @SerializedName(value = "expirationTime", alternate = {"expires_in"})
    private long expirationTime;

    public SessionInfo(String secret, String token) {
        this(secret, token, Long.MAX_VALUE);
    }

    public SessionInfo(String secret, String token, long expirationSeconds) {
        this.sessionSecret = secret;
        this.sessionToken = token;
        this.expirationTime = expirationSeconds;
        validateExpirationTime();
    }


    public boolean isValid() {
        return (this.sessionToken != null && this.sessionSecret != null && System.currentTimeMillis() < this.expirationTime);
    }

    /*
    Validate various unlimited expiration time values and sets them to the required specification.
     */
    private void validateExpirationTime() {
        if (expirationTime == 0 || expirationTime == -1 || expirationTime == Long.MAX_VALUE)
            expirationTime = Long.MAX_VALUE;
    }

    public boolean isSetToExpire() {
        return expirationTime != Long.MAX_VALUE;
    }

    @Override
    public void onPostGsonProcess() {
        validateExpirationTime();
    }

    @NonNull
    @Override
    public String toString() {
        return "Secret:  " + this.sessionSecret + "\n" + "Token: " + this.sessionToken + "\n" + "ExpirationTime: " + this.expirationTime;
    }

    //region Getters & Setters

    public String getSessionToken() {
        return sessionToken;
    }

    public String getSessionSecret() {
        return sessionSecret;
    }

    public long getExpirationTime() {
        return expirationTime;
    }

    //endregion
}
