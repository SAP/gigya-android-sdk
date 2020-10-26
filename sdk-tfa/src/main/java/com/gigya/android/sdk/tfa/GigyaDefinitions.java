package com.gigya.android.sdk.tfa;

import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class GigyaDefinitions {

    public static final int PUSH_TFA_CONTENT_ACTION_REQUEST_CODE = 2020;
    public static final int PUSH_TFA_CONTENT_INTENT_REQUEST_CODE = 2021;

    public static final String TFA_CHANNEL_ID = "tfa_channel";

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

        // Push
        public static final String API_TFA_PUSH_OPT_IN = "accounts.tfa.push.optin";
        public static final String API_TFA_PUSH_VERIFY = "accounts.tfa.push.verify";
        public static final String API_TFA_SEND_VERIFICATION = "accounts.tfa.push.sendVerification";
    }
    
    public static class TFAProvider {

        @Retention(RetentionPolicy.SOURCE)
        @StringDef({EMAIL, PHONE, LIVELINK, TOTP, PUSH})
        public @interface Provider {

        }

        public static final String EMAIL = "gigyaEmail";
        public static final String PHONE = "gigyaPhone";
        public static final String LIVELINK = "livelink";
        public static final String TOTP = "gigyaTotp";
        public static final String PUSH = "gigyaPush";
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
