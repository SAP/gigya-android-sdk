package com.gigya.android.sdk.tfa;

import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class GigyaDefinitions {

    public static class API {

        public static final String API_TFA_INIT = "accounts.tfa.initTFA";
        public static final String API_TFA_FINALIZE = "accounts.tfa.finalizeTFA";
        public static final String API_TFA_EMAIL_GET_EMAILS = "accounts.tfa.email.getEmails";
        public static final String API_TFA_EMAIL_SEND_VERIFICATION_CODE = "accounts.tfa.email.sendVerificationCode";
        public static final String API_TFA_EMAIL_COMPLETE_VERIFICATION = "accounts.tfa.email.completeVerification";
        public static final String API_TFA_PHONE_COMPLETE_VERIFICATION = "accounts.tfa.phone.completeVerification";
        public static final String API_TFA_PHONE_GET_REGISTERED_NUMBERS = "accounts.tfa.phone.getRegisteredPhoneNumbers";
        public static final String API_TFA_PHONE_SEND_VERIFICATION_CODE = "accounts.tfa.phone.sendVerificationCode";
        public static final String API_TFA_TOTP_REGISTER = "accounts.tfa.totp.register";
        public static final String API_TFA_TOTP_VERIFY = "accounts.tfa.totp.verify";
    }

    /**
     * TFA related specific constants.
     */
    public static class TFAProvider {

        @Retention(RetentionPolicy.SOURCE)
        @StringDef({EMAIL, PHONE, LIVELINK, TOTP})
        public @interface Provider {

        }

        public static final String EMAIL = "gigyaEmail";
        public static final String PHONE = "gigyaPhone";
        public static final String LIVELINK = "liveLink";
        public static final String TOTP = "gigyaTotp";
    }

    public static class PhoneMethod {

        @Retention(RetentionPolicy.SOURCE)
        @StringDef({SMS, VOICE})

        public @interface Method {

        }

        public static final String SMS = "sms";
        public static final String VOICE = "voice";
    }

}
