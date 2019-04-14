package com.gigya.android.sdk.ui.plugin;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.GigyaPluginCallback;
import com.gigya.android.sdk.api.IBusinessApiService;

import java.util.Map;

public interface IWebViewFragmentFactory {

    void showPluginFragment(AppCompatActivity activity, Bundle args, final GigyaPluginCallback gigyaPluginCallback);

    void showProviderFragment(AppCompatActivity activity, IBusinessApiService businessApiService, final Map<String, Object> params, Bundle args, final GigyaLoginCallback gigyaLoginCallback);
}
