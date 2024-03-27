package com.gigya.android.sdk.ui;

import androidx.appcompat.app.AppCompatActivity;

import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.GigyaPluginCallback;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.ui.plugin.GigyaPluginBaseFragment;

import java.util.List;
import java.util.Map;

public interface IPresenter<A extends GigyaAccount> {

    void showPlugin(boolean obfuscate, final String plugin, boolean fullScreen, final Map<String, Object> params, final GigyaPluginCallback<A> callback);

    void showNativeLoginProviders(@GigyaDefinitions.Providers.SocialProvider List<String> providers, IBusinessApiService businessApiService,
                                  Map<String, Object> params, GigyaLoginCallback<A> gigyaLoginCallback);

    String getPresentationUrl(Map<String, Object> params, String requestType);

    GigyaPluginBaseFragment<A> getFragmentPlugin(AppCompatActivity activity, boolean obfuscate, final String plugin, boolean fullScreen, final Map<String, Object> params, final GigyaPluginCallback<A> callback);
}
