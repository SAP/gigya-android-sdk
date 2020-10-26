package com.gigya.android.sdk.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Random;
import java.util.TreeMap;

public class AuthUtils {

    /**
     * Add relevant authentication parameters to request parameters.
     *
     * @param sessionSecret Active session secret.
     * @param httpMethod    Http request method.
     * @param baseUrl       Base url for request.
     * @param params        Active request parameters.
     * @param offset        Server click offset.
     */
    public static void addAuthenticationParameters(final String sessionSecret,
                                                   int httpMethod,
                                                   String baseUrl,
                                                   @NonNull final TreeMap<String, Object> params,
                                                   @Nullable Long offset) {
        final String timestamp = Long.toString((System.currentTimeMillis() / 1000) + (offset != null ? offset : 0));
        // Add timestamp.
        params.put("timestamp", timestamp);

        // Add nonce.
        final Random random = new Random();
        String nonce = System.currentTimeMillis() + "_" + random.nextInt();
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

    /**
     * Removing authentication specific parameters.
     * This step is crucial when resending the same request.
     *
     * @param params Request parameters map.
     */
    public static void removeAuthenticationParameters(@NonNull final TreeMap<String, Object> params) {
        params.remove("sig");
        params.remove("timestamp");
        params.remove("nonce");
    }
}
