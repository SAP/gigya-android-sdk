package com.gigya.android.sdk.utils;

import com.gigya.android.sdk.Gigya;

public class EnvUtils {

    public static void checkGson() {
        try {
            Class.forName("com.google.gson.Gson");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("Gson library is missing. Make sure you have added it as instructed in documentation.");
        }
    }
}
