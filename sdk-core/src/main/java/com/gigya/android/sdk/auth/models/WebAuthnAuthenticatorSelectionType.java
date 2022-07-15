package com.gigya.android.sdk.auth.models;

import androidx.annotation.NonNull;

import com.google.android.gms.fido.fido2.api.common.Attachment;

public enum WebAuthnAuthenticatorSelectionType {
    PLATFORM(Attachment.PLATFORM) {
        @NonNull
        public String toString() {
            return "Platform";
        }
    },
    CROSS_PLATFORM(Attachment.CROSS_PLATFORM) {
        @NonNull
        public String toString() {
            return "Cross-Platform";
        }
    },
    UNSPECIFIED(Attachment.PLATFORM) {
        @NonNull
        public String toString() {
            return "Unspecified";
        }
    };

    private final Attachment attachment;

    WebAuthnAuthenticatorSelectionType(Attachment attachment) {
        this.attachment = attachment;
    }

    public Attachment attachment() {
        return this.attachment;
    }
}
