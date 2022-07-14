package com.gigya.android.sdk.auth.models;

import androidx.annotation.NonNull;

import com.google.android.gms.fido.fido2.api.common.Attachment;

public enum WebAuthnAuthenticatorSelectionType {
    PLATFORM {
        @NonNull
        public String toString() {
            return "Platform";
        }

        public Attachment getAttachment() {
            return Attachment.PLATFORM;
        }
    },
    CROSS_PLATFORM {
        @NonNull
        public String toString() {
            return "Cross-Platform";
        }

        public Attachment getAttachment() {
            return Attachment.CROSS_PLATFORM;
        }
    },
    UNSPECIFIED {
        @NonNull
        public String toString() {
            return "Unspecified";
        }

        public Attachment getAttachment() {
            return Attachment.PLATFORM;
        }
    }
}
