package com.gigya.android.sdk;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.utils.FileUtils;
import com.google.gson.Gson;

public class Config {

    private String apiKey;
    private String apiDomain;
    private String gmid;
    private String ucid;
    private int accountCacheTime;
    private boolean interruptionsEnabled = true;
    private int sessionVerificationInterval = 0;

    //region LOAD

    @Nullable
    public static Config loadFromJson(Context appContext) {
        if (FileUtils.containsConfigJSON(appContext)) {
            try {
                String json = FileUtils.loadConfigJSON(appContext);
                GigyaLogger.debug("Configuration", json);
                return new Gson().fromJson(json, Config.class);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    @Nullable
    public static Config loadFromManifest(Context appContext) {
        try {
            ApplicationInfo ai = appContext.getPackageManager().getApplicationInfo(appContext.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            final String apiKey = bundle.getString("apiKey", null);
            final String domain = bundle.getString("apiDomain", "us1.gigya.com");
            final int accountCacheTime = bundle.getInt("accountCacheTime", 5);
            final int sessionVerificationIInterval = bundle.getInt("sessionVerificationInterval", 0);
            return new Config().updateWith(apiKey, domain, accountCacheTime, sessionVerificationIInterval);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    //endregion

    //region UPDATE

    public Config updateWith(String apiKey, String apiDomain) {
        this.apiKey = apiKey;
        this.apiDomain = apiDomain;
        return this;
    }

    public Config updateWith(String apiKey, String apiDomain, int accountCacheTime, int sessionVerificationInterval) {
        this.apiKey = apiKey;
        this.apiDomain = apiDomain;
        this.accountCacheTime = accountCacheTime;
        this.sessionVerificationInterval = sessionVerificationInterval;
        return this;
    }

    public Config updateWith(Config config) {
        updateWith(
                config.getApiKey(),
                config.getApiDomain(),
                config.getAccountCacheTime(),
                config.getSessionVerificationInterval()
        );
        if (config.getGmid() != null) {
            this.gmid = config.getGmid();
        }
        if (config.getUcid() != null) {
            this.ucid = config.getUcid();
        }
        return this;
    }

    //endregion

    //region GETTERS & SETTERS

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiDomain() {
        return apiDomain;
    }

    public void setApiDomain(String apiDomain) {
        this.apiDomain = apiDomain;
    }

    public String getGmid() {
        return gmid;
    }

    public void setGmid(String gmid) {
        this.gmid = gmid;
    }

    public String getUcid() {
        return ucid;
    }

    public void setUcid(String ucid) {
        this.ucid = ucid;
    }

    public int getAccountCacheTime() {
        return accountCacheTime;
    }

    public void setAccountCacheTime(int accountCacheTime) {
        this.accountCacheTime = accountCacheTime;
    }

    public boolean isInterruptionsEnabled() {
        return interruptionsEnabled;
    }

    public void setInterruptionsEnabled(boolean interruptionsEnabled) {
        this.interruptionsEnabled = interruptionsEnabled;
    }

    public int getSessionVerificationInterval() {
        return sessionVerificationInterval;
    }

    public void setSessionVerificationInterval(int sessionVerificationInterval) {
        this.sessionVerificationInterval = sessionVerificationInterval;
    }

    //endregion
}
