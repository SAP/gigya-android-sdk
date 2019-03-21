package com.gigya.android.sdk.biometric;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.biometric.utils.GigyaBiometricUtils;
import com.gigya.android.sdk.biometric.v23.GigyaBiometricV23;
import com.gigya.android.sdk.biometric.v28.GigyaBiometricV28;


public abstract class GigyaBiometric implements IGigyaBiometricActions {

    private static final String LOG_TAG = "GigyaBiometric";

    protected static final String FINGERPRINT_KEY_NAME = "fingerprint";

    @Nullable
    protected String title;
    @Nullable
    protected String subtitle;
    @Nullable
    protected String description;

    public GigyaBiometric(@Nullable String title, @Nullable String subtitle, @Nullable String description) {
        this.title = title;
        this.subtitle = subtitle;
        this.description = description;
    }

    public static class Builder {

        @Nullable
        private String title;
        @Nullable
        private String subtitle;
        @Nullable
        private String description;

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder subtitle(String subtitle) {
            this.subtitle = subtitle;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        @NonNull
        public GigyaBiometric build() {
            if (GigyaBiometricUtils.isPromptEnabled()) {
                return new GigyaBiometricV28(title, subtitle, description);
            } else {
                return new GigyaBiometricV23(title, subtitle, description);
            }
        }
    }

    public void optIn(Context context, IGigyaBiometricCallback callback) {
        GigyaLogger.debug(LOG_TAG, "optIn: ");
        showPrompt(context, callback, new Runnable() {
            @Override
            public void run() {
                // Stub.
            }
        });
    }

    public void optOut(Context context, IGigyaBiometricCallback callback) {
        GigyaLogger.debug(LOG_TAG, "optOut: ");
        showPrompt(context, callback, new Runnable() {
            @Override
            public void run() {
                // Stub.
            }
        });
    }

    public void lock(Context context, IGigyaBiometricCallback callback) {
        GigyaLogger.debug(LOG_TAG, "lock: ");
        showPrompt(context, callback, new Runnable() {
            @Override
            public void run() {
                // Lock the session.
            }
        });
    }

    public void unlock(Context context, IGigyaBiometricCallback callback) {
        GigyaLogger.debug(LOG_TAG, "unlock: ");
        showPrompt(context, callback, new Runnable() {
            @Override
            public void run() {
                // Unlock the session.
            }
        });
    }
}
