package com.gigya.android.sdk;

import com.gigya.android.sdk.account.GigyaAccountConfig;
import com.google.gson.annotations.SerializedName;

public class Config {

    private String apiKey;
    private String apiDomain;
    private String gmid;
    private String ucid;

    @Deprecated
    // Will be removed in SDK code version 6.
    private int accountCacheTime;

    private boolean interruptionsEnabled = true;
    private int sessionVerificationInterval = 0;
    private Long serverOffset;
    private boolean secureActivityWindow = false;

    @SerializedName("account")
    private GigyaAccountConfig gigyaAccountConfig;

    private String cname;
    private boolean cnameEnabled = false;

    //region UPDATE

    public Config updateWith(String apiKey, String apiDomain) {
        this.apiKey = apiKey;
        this.apiDomain = apiDomain;
        return this;
    }

    public Config updateWith(String apiKey, String apiDomain, String cname) {
        this.apiKey = apiKey;
        this.apiDomain = apiDomain;
        this.cname = cname;
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
        if (config == null) {
            return this;
        }
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
        if (config.getGigyaAccountConfig() != null) {
            this.gigyaAccountConfig = config.getGigyaAccountConfig();
        }
        if (config.getCname() != null) {
            this.cname = config.getCname();
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
        // Account configuration object gets priority.
        if (gigyaAccountConfig != null) {
            return gigyaAccountConfig.getCacheTime();
        }
        // Will be removed in SDK code version 6.
        return accountCacheTime;
    }

    @Deprecated
    // Will be removed in SDK code version 6.
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

    public Long getServerOffset() {
        return serverOffset;
    }

    public void setServerOffset(Long serverOffset) {
        this.serverOffset = serverOffset;
    }

    public boolean isSecureActivities() {
        return secureActivityWindow;
    }

    public void setSecureActivities(boolean secureActivities) {
        this.secureActivityWindow = secureActivities;
    }

    public GigyaAccountConfig getGigyaAccountConfig() {
        return gigyaAccountConfig;
    }

    public void setGigyaAccountConfig(GigyaAccountConfig gigyaAccountConfig) {
        this.gigyaAccountConfig = gigyaAccountConfig;
    }

    public String getCname() {
        return cname;
    }

    public void setCname(String cname) {
        this.cname = cname;
    }

    public boolean isCnameEnabled() {
        return cname != null;
    }

    public void setCnameEnabled(boolean cnameEnabled) {
        this.cnameEnabled = cnameEnabled;
    }

    //endregion
}
