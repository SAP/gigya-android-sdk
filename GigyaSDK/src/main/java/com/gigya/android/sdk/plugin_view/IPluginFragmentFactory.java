package com.gigya.android.sdk.plugin_view;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.gigya.android.sdk.GigyaPluginCallback;

public interface IPluginFragmentFactory {

    void showFragment(AppCompatActivity activity, Bundle args, GigyaPluginCallback gigyaPluginCallback);
}
