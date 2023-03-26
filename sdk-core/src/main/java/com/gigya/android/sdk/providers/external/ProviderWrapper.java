package com.gigya.android.sdk.providers.external;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.Nullable;

public class ProviderWrapper {

    protected String pId;

    public ProviderWrapper(Context context, int identifier) {
//        pId = providerIdFromMetaData(context, identifier);
        pId = context.getString(identifier);
    }

    @Nullable
    public String providerIdFromMetaData(Context context, String identifier) {
        String clientId = null;
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle metaData = appInfo.metaData;
            if (metaData.get(identifier) instanceof String) {
                clientId = (String) metaData.get(identifier);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return clientId;
    }
}
