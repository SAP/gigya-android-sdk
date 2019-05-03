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

public class ConfigFactory {
    private Context _appContext;

    public ConfigFactory(Context appContext) {
        _appContext = appContext;
    }

    public Config load() {
        Config config = loadFromJson();
        if (config == null) {
            config = loadFromManifest();
        }

        return config;
    }

    @Nullable
    public Config loadFromJson() {
        if (FileUtils.containsConfigJSON(_appContext)) {
            try {
                String json = FileUtils.loadConfigJSON(_appContext);
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
    public Config loadFromManifest() {
        try {
            ApplicationInfo ai = _appContext.getPackageManager().getApplicationInfo(_appContext.getPackageName(), PackageManager.GET_META_DATA);
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
}
