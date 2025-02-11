package com.gigya.android.sdk.ui.plugin;

import com.gigya.android.sdk.network.GigyaError;

public interface IGigyaPluginFileErrorCallback {

    void onFileError(GigyaError error);
}
