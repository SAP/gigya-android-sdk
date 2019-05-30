package com.gigya.android.sdk.ui.plugin;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.GigyaPluginCallback;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.api.IBusinessApiService;

import java.util.Map;

public interface IWebViewFragmentFactory<A extends GigyaAccount> {

    void showPluginFragment(AppCompatActivity activity, String plugin, final Map<String, Object> params, Bundle args, final GigyaPluginCallback<A> gigyaPluginCallback);

    void showProviderFragment(AppCompatActivity activity, IBusinessApiService businessApiService, final Map<String, Object> params, Bundle args, final GigyaLoginCallback gigyaLoginCallback);
}
