package com.gigya.android.sdk.model;

import com.google.gson.annotations.SerializedName;

public class SessionInfo {

    private String sessionToken;
    private String sessionSecret;
    @SerializedName(value = "expirationTime", alternate = {"expires_in"})
    private long expirationTime = Long.MAX_VALUE;

    public SessionInfo(String secret, String token) {
        this(secret, token, Long.MAX_VALUE);
    }

    public SessionInfo(String secret, String token, long expirationSeconds) {
        this.sessionSecret = secret;
        this.sessionToken = token;
        this.expirationTime = expirationSeconds;
        validateExpirationTime();
    }

    public void validateExpirationTime() {
        if (expirationTime == 0 || expirationTime == -1 || expirationTime == Long.MAX_VALUE)
            expirationTime = Long.MAX_VALUE;
    }

    @Override
    public String toString() {
        return "\n" +
                "Secret:  " + this.sessionSecret + "\n" +
                "Token: " + this.sessionToken + "\n" +
                "ExpirationTime: " + this.expirationTime +
                "\n";
    }

    public boolean isValid() {
        return (this.sessionToken != null && this.sessionSecret != null && System.currentTimeMillis() < this.expirationTime);
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
