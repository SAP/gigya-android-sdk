package com.gigya.android.sdk.ui.new_plugin_impl;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

public class GigyaPluginFileChooser {

    private static final int PERMISSION_REQUEST_CODE = 14;
    private static final int FILE_CHOOSER_MEDIA_REQUEST_CODE = 15;
    private String _cameraTempImagePath;
    private ValueCallback<Uri[]> _imagePathCallback;

    public void show(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
        if (_imagePathCallback != null) {
            _imagePathCallback.onReceiveValue(null);
        }
    }

    public void onActivityResult(int resultCode, Intent data) {

    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

    }
}
