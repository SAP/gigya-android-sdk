package com.gigya.android.sdk.session;

import com.google.gson.annotations.SerializedName;

public class SessionInfo{

    private String sessionToken;
    private String sessionSecret;
    @SerializedName(value = "expirationTime", alternate = {"expires_in"})
    private long expirationTime; // In seconds.

    public SessionInfo(String secret, String token) {
        this(secret, token, 0);
    }

    public SessionInfo(String secret, String token, long expirationSeconds) {
        this.sessionSecret = secret;
        this.sessionToken = token;
        this.expirationTime = expirationSeconds;
    }

    public boolean isValid() {
        return (this.sessionToken != null && this.sessionSecret != null);
    }

    //region GETTERS & SETTERS

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
