package com.gigya.android.sdk.utils;

import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.reporting.ISentReport;
import com.gigya.android.sdk.reporting.ReportingManager;

public class EnvUtils {

    public static void checkGson() {
        try {
            Class.forName("com.google.gson.Gson");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            ReportingManager.get().runtimeException(Gigya.VERSION, "core", "Gson library is missing", null, new ISentReport() {
                @Override
                public void done() {
                    throw new RuntimeException("Gson library is missing. Make sure you have added it as instructed in documentation.");
                }
            });
        }
    }
}
