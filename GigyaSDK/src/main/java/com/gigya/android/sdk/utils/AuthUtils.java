package com.gigya.android.sdk.utils;

import android.support.annotation.NonNull;

import java.util.Random;
import java.util.TreeMap;

public class AuthUtils {

    public static void addAuthenticationParameters(final String sessionSecret, int httpMethod, String baseUrl, @NonNull final TreeMap<String, Object> params) {
        final String timestamp = Long.toString((System.currentTimeMillis() / 1000));
        // Add timestamp.
        params.put("timestamp", timestamp);

        // Add nonce.
        final Random random = new Random();
        String nonce = Long.toString(System.currentTimeMillis()) + "_" + random.nextInt();
        params.put("nonce", nonce);

        // Add signature.
        final String signature = SigUtils.getSignature(
                sessionSecret,
                httpMethod == 1 ? "POST" : "GET",
                baseUrl,
                params);
        if (signature != null) {
            params.put("sig", signature);
        }
    }
}
