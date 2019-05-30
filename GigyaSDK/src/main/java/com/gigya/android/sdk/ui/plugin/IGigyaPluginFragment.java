package com.gigya.android.sdk.ui.plugin;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.view.View;

import com.gigya.android.sdk.GigyaPluginCallback;
import com.gigya.android.sdk.account.models.GigyaAccount;

public interface IGigyaPluginFragment<A extends GigyaAccount> {

    void setCallback(final GigyaPluginCallback<A> gigyaPluginCallback);

    void setHtml(final String html);

    void setUpUiElements(final View fragmentView);

    void setUpWebViewElement();

    void loadUrl(final View fragmentView);

    void dismissWhenDone();

    void evaluateActivityResult(int requestCode, int resultCode, Intent data);

    void evaluatePermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults);

}
