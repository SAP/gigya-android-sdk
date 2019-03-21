package com.gigya.android.sdk.biometric;

import android.content.Context;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.biometric.utils.BiomerticUtils;
import com.gigya.android.sdk.biometric.v23.GigyaBiometricV23;
import com.gigya.android.sdk.biometric.v28.GigyaBiometricV28;


public abstract class GigyaBiometric {

    protected static final String FINGERPRINT_KEY_NAME = "fingerprint";

    protected IGigyaBiometricCallback _callback;

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

        public GigyaBiometric build() {
            if (BiomerticUtils.isPromptEnabled()) {
                new GigyaBiometricV28(title, subtitle, description);
            } else {
                new GigyaBiometricV23(title, subtitle, description);
            }
            return null;
        }
    }

    public abstract void optIn(Context context, IGigyaBiometricCallback callback);

    protected abstract void displayBiometricDialog();
}
