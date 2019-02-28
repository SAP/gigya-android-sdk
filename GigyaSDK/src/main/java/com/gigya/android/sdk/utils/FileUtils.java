package com.gigya.android.sdk.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;

public class FileUtils {

    /*
    Load JSON configuration file from application Assets folder.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static String loadConfigurationJSON(Context appContext) throws IOException {
        return assetJsonFileToString(appContext, "gigyaSdkConfiguration.json");
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static String assetJsonFileToString(Context appContext, String fileName) throws IOException {
        InputStream is = appContext.getAssets().open(fileName);
        return streamToString(is);
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored", "CharsetObjectCanBeUsed"})
    public static String streamToString(InputStream is) throws IOException {
        final int size = is.available();
        byte[] buffer = new byte[size];
        is.read(buffer);
        is.close();
        return new String(buffer, "UTF-8");
    }

    @Nullable
    public static String stringFromMetaData(Context context, String fieldName) {
        String field = null;
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            field = (String) appInfo.metaData.get(fieldName);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return field;
    }
}
