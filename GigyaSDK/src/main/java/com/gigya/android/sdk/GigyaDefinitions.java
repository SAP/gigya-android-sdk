package com.gigya.android.sdk;

import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * SDK global definitions.
 * Contains SDK relevant global and specific constants.
 */
public class GigyaDefinitions {

    /**
     * Local broadcast specific constants.
     */
    public static class Broadcasts {
        // ACTIONS
        public static final String INTENT_ACTION_SESSION_EXPIRED = "intent_action_session_expired";
        public static final String INTENT_ACTION_SESSION_INVALID = "intent_action_session_invalid";
    }

    /**
     * Api related specific constants.
     */
    public static class API {
        // ACCOUNT
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
        public static final String API_GET_CONFLICTING_ACCOUNTS = "accounts.getConflictingAccount";
        public static final String API_NOTIFY_LOGIN = "accounts.notifyLogin";
        public static final String API_VERIFY_LOGIN = "accounts.verifyLogin";
        public static final String API_NOTIFY_SOCIAL_LOGIN = "accounts.notifySocialLogin";
        public static final String API_REMOVE_CONNECTION = "socialize.removeConnection";
        // TFA
        public static final String API_TFA_INIT = "accounts.tfa.initTFA";
        public static final String API_TFA_GET_PROVIDERS = "accounts.tfa.getProviders";
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
    public static class TFA {

        @Retention(RetentionPolicy.SOURCE)
        @StringDef({EMAIL, PHONE, TOTP})
        public @interface TFAProvider {

        }

        public static final String EMAIL = "gigyaEmail";
        public static final String PHONE = "gigyaPhone";
        public static final String TOTP = "gigyaTotp";
    }

    /**
     * Plugin view specific constants.
     */
    public static class Plugin {

        @Retention(RetentionPolicy.SOURCE)
        @StringDef({FINISHED, CANCELED})
        public @interface PluginReason {

        }

        public static final String FINISHED = "finished";
        public static final String CANCELED = "canceled";
    }

    /**
     * Supported login providers specific constants.
     */
    public static class Providers {

        @Retention(RetentionPolicy.SOURCE)
        @StringDef({AMAZON, BLOGGER, FACEBOOK, FOURSQUARE, GOOGLE, INSTAGRAM, KAKAO, LINE, LINKEDIN, WECHAT,
                LIVEDOOR, MESSENGER, MIXI, NAVER, NETLOG, ODNOKLASSNIKI, ORANGE_FRANCE, PAYPALOAUTH, TENCENT_QQ, RENREN, SINA_WEIBO,
                SPICEWORKS, TWITTER, VKONTAKTE, WECHAT, WORDPRESS, XING, YAHOO, YAHOO_JAPAN})
        public @interface SocialProvider {
        }

        public static final String AMAZON = "amazon";
        public static final String BLOGGER = "blogger";
        public static final String FACEBOOK = "facebook";
        public static final String FOURSQUARE = "foursquare";
        public static final String GOOGLE = "googleplus";
        public static final String INSTAGRAM = "Instagram";
        public static final String KAKAO = "kakao";
        public static final String LINE = "line";
        public static final String LINKEDIN = "linkedin";
        public static final String LIVEDOOR = "livedoor";
        public static final String MESSENGER = "messenger";
        public static final String MIXI = "mixi";
        public static final String NAVER = "naver";
        public static final String NETLOG = "netlog";
        public static final String ODNOKLASSNIKI = "odnoklassniki";
        public static final String ORANGE_FRANCE = "orange france";
        public static final String PAYPALOAUTH = "paypaloauth";
        public static final String TENCENT_QQ = "tencent qq";
        public static final String RENREN = "renren";
        public static final String SINA_WEIBO = "sina weibo";
        public static final String SPICEWORKS = "spiceworks";
        public static final String TWITTER = "twitter";
        public static final String VKONTAKTE = "vkontakte";
        public static final String WECHAT = "wechat";
        public static final String WORDPRESS = "wordpress";
        public static final String XING = "xing";
        public static final String YAHOO = "yahoo";
        public static final String YAHOO_JAPAN = "Yahoo Japan";
    }

}
