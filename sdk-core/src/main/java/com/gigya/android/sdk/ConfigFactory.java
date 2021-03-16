package com.gigya.android.sdk;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.gigya.android.sdk.utils.FileUtils;
import com.google.gson.Gson;

public class ConfigFactory {

    private FileUtils _fileUtils;

    public ConfigFactory(FileUtils fileUtils) {
        _fileUtils = fileUtils;
    }

    @Nullable
    public Config load() {
        Config config = loadFromJson();
        if (config == null) {
            config = loadFromManifest();
        }
        return config;
    }

    @Nullable
    public Config loadFromJson() {
        String configFileName = "gigyaSdkConfiguration.json";
        if (_fileUtils.containsFile(configFileName)) {
            try {
                String json = _fileUtils.loadFile(configFileName);
                GigyaLogger.debug("Configuration", json);
                return new Gson().fromJson(json, Config.class);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }

    // Will be removed in SDK code version 6.
    @Deprecated
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    @Nullable
    public Config loadFromManifest() {
        Bundle bundle = _fileUtils.getMetaData();
        if (bundle == null) {
            return null;
        }
        final String apiKey = bundle.getString("apiKey", null);
        final String domain = bundle.getString("apiDomain", "us1.gigya.com");
        final int accountCacheTime = bundle.getInt("accountCacheTime", 5);
        final int sessionVerificationIInterval = bundle.getInt("sessionVerificationInterval", 0);
        return new Config().updateWith(apiKey, domain, accountCacheTime, sessionVerificationIInterval);
    }
}
