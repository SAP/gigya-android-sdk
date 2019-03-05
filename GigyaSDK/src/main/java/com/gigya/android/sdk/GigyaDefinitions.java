package com.gigya.android.sdk;

/**
 * SDK global definitions.
 */
public class GigyaDefinitions {

    public static class API {
        public static final String API_GET_SDK_CONFIG = "socialize.getSDKConfig";
        public static final String API_LOGIN = "accounts.login";
        public static final String API_LOGOUT = "accounts.logout";
        public static final String API_GET_ACCOUNT_INFO = "accounts.getAccountInfo";
        public static final String API_SET_ACCOUNT_INFO = "accounts.setAccountInfo";
        public static final String API_RESET_PASSWORD = "accounts.resetPassword";
        public static final String API_REFRESH_PROVIDER_SESSION = "socialize.refreshProviderSession";
        public static final String API_INIT_REGISTRATION = "accounts.initRegistration";
        public static final String API_REGISTER = "accounts.register";
        public static final String API_FINALIZE_REGISTRATION = "accounts.finalizeRegistration";

        public static final String API_TFA_INIT = "accounts.tfa.initTFA";
        public static final String API_TFA_GET_PROVIDERS = "accounts.tfa.getProviders";
        public static final String API_TFA_FINALIZE = "accounts.tfa.finalizeTFA";
        public static final String API_TFA_EMAIL_GET_EMAILS = "accounts.tfa.email.getEmails";
        public static final String API_TFA_EMAIL_SEND_VERIFICATION_CODE = "accounts.tfa.email.sendVerificationCode";
        public static final String API_TFA_PNONE_COMPLETE_VERIFICATION = "accounts.tfa.phone.completeVerification";
        public static final String API_TFA_PHONE_GET_REGISTERED_NUMBERS = "accounts.tfa.phone.getRegisteredPhoneNumbers";
        public static final String API_TFA_PHONE_SEND_VERIFICATION_CODE = "accounts.tfa.phone.sendVerificationCode";
        public static final String API_TFA_TOTP_REGISTER = "accounts.tfa.totp.register";
        public static final String API_TFA_TOTP_VERIFY = "accounts.tfa.totp.verify";
    }

    public static class TFA {

        public static final String EMAIL = "gigyaEmail";
        public static final String PHONE = "gigyaPhone";
        public static final String TOTP = "gigyaTotp";
    }

}
