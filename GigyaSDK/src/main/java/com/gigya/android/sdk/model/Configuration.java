package com.gigya.android.sdk.model;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.gigya.android.sdk.log.GigyaLogger;
import com.gigya.android.sdk.utils.FileUtils;
import com.google.gson.Gson;

import java.util.Map;

public class Configuration {

    private String apiKey;
    private String apiDomain;
    private IDs IDs = new IDs();
    private int accountCacheTime = 5;
    private Map<String, String> appIds;

    public Configuration() {

    }

    public Configuration(String apiKey, String domain, int accountCacheTime) {
        this.apiKey = apiKey;
        this.apiDomain = domain;
        this.accountCacheTime = accountCacheTime;
    }

    public void update(String apiKey, String domain) {
        this.apiKey = apiKey;
        this.apiDomain = domain;
    }

    public void update(Configuration configuration) {
        update(configuration.getApiKey(), configuration.getApiDomain());
        if (configuration.hasGMID()) {
            setIDs(configuration.getIDs());
        }
        if (configuration.getAppIds() != null) {
            setAppIds(configuration.getAppIds());
        }
        accountCacheTime = configuration.getAccountCacheTime();
    }

    public void updateIds(String ucid, String gmid) {
        getIDs().update(ucid, gmid);
    }

    public boolean hasApiKey() {
        return !TextUtils.isEmpty(this.apiKey);
    }

    public boolean hasGMID() {
        return !TextUtils.isEmpty(getIDs().gmid);
    }

    public boolean isSynced() {
        return appIds != null;
    }

    //region Getters & Setters

    public String getApiKey() {
        return apiKey;
    }

    public String getApiDomain() {
        return apiDomain;
    }

    public String getGMID() {
        return getIDs().getGmid();
    }

    public String getUCID() {
        return getIDs().getUcid();
    }

    public IDs getIDs() {
        return IDs;
    }

    public void setIDs(IDs IDs) {
        this.IDs = IDs;
    }

    public int getAccountCacheTime() {
        return accountCacheTime;
    }

    public void setAccountCacheTime(int accountCacheTime) {
        this.accountCacheTime = accountCacheTime;
    }

    public Map<String, String> getAppIds() {
        return appIds;
    }

    public void setAppIds(Map<String, String> appIds) {
        this.appIds = appIds;
    }

    //endregion

    //region Load configuration implicitly

    @Nullable
    public static Configuration loadFromJson(Context appContext) {
        try {
            String json = FileUtils.loadConfigurationJSON(appContext);
            GigyaLogger.debug("Configuration", json);
            return new Gson().fromJson(json, Configuration.class);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private static final String KEY_META_API_KEY = "apiKey";
    private static final String KEY_META_DOMAIN = "apiDomain";
    private static final String KEY_ACCOUNT_CACHE_TIME = "accountCacheTime";

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    @Nullable
    public static Configuration loadFromManifest(Context appContext) {
        try {
            ApplicationInfo ai = appContext.getPackageManager().getApplicationInfo(appContext.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            final String apiKey = bundle.getString(KEY_META_API_KEY, null);
            final String domain = bundle.getString(KEY_META_DOMAIN, "us1.gigya.com");
            final int accountCacheTime = bundle.getInt(KEY_ACCOUNT_CACHE_TIME, 5);
            return new Configuration(apiKey, domain, accountCacheTime);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    //endregion

    public static final class IDs {

        private String gmid;
        private String ucid;

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

        public void update(String ucid, String gmid) {
            this.ucid = ucid;
            this.gmid = gmid;
        }
    }
}
