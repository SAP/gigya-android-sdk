package com.gigya.android.sdk.ui.plugin;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.GigyaPluginCallback;

import java.util.Map;

public interface IWebViewFragmentFactory {

    void showPluginFragment(AppCompatActivity activity, Bundle args, final GigyaPluginCallback gigyaPluginCallback);

    void showProviderFragment(AppCompatActivity activity, final Map<String, Object> params, Bundle args, final GigyaLoginCallback gigyaLoginCallback);
}
