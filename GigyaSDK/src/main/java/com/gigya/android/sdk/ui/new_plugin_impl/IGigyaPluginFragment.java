package com.gigya.android.sdk.ui.new_plugin_impl;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.view.View;

import com.gigya.android.sdk.GigyaPluginCallback;

public interface IGigyaPluginFragment {

    void setCallback(final GigyaPluginCallback gigyaPluginCallback);

    void setHtml(final String html);

    void setUpUiElements(final View fragmentView);

    void setUpWebViewElement();

    void loadUrl(final View fragmentView);

    void dismissWhenDone();

    void evaluateActivityResult(int requestCode, int resultCode, Intent data);

    void evaluatePermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults);

}
