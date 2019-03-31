package com.gigya.android.sdk;

import android.content.Context;

import com.gigya.android.sdk.services.Config;

public interface IGigyaContext {

    Context getContext();

    Config getConfig();

    void updateConfig(Config newConfig);

}
