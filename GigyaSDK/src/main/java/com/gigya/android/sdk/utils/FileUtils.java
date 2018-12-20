package com.gigya.android.sdk.utils;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;

public class FileUtils {

    /*
    Load JSON configuration file from application Assets folder.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static String loadConfigurationJSON(Context appContext) throws IOException {
        return assetJsonFileToString(appContext, "gigya-sdk-configuration.json");
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static String assetJsonFileToString(Context appContext, String fileName) throws IOException {
        InputStream is = appContext.getAssets().open(fileName);
        return streamToString(is);
    }

    public static String streamToString(InputStream is) throws IOException {
        final int size = is.available();
        byte[] buffer = new byte[size];
        is.read(buffer);
        is.close();
        return new String(buffer, "UTF-8");
    }
}
