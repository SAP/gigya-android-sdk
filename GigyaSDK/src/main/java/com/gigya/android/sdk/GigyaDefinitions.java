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
        public static final String API_TFA_GET_PROVIDERS = "accounts.tfa.getProviders";
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

    public static class AccountIncludes {

        @Retention(RetentionPolicy.SOURCE)
        @StringDef({IDENTITIES_ACTIVE, IDENTITIES_ALL, IDENTITIES_GLOBAL, LOGIN_IDS, EMAILS, PROFILE, DATA, PASSWORD, IS_LOCKED_OUT,
                LAST_LOGIN_LOCATION, REG_SOURCE, I_RANK, RBA, SUBSCRIPTIONS, USER_INFO, PREFERENCES, GROUPS})
        public @interface Includes {

        }

        public static final String IDENTITIES_ACTIVE = "identities-active";
        public static final String IDENTITIES_ALL = "identities-all";
        public static final String IDENTITIES_GLOBAL = "identities-global";
        public static final String LOGIN_IDS = "loginIDs";
        public static final String EMAILS = "emails";
        public static final String PROFILE = "profile";
        public static final String DATA = "data";
        public static final String PASSWORD = "password";
        public static final String IS_LOCKED_OUT = "isLockedOut";
        public static final String LAST_LOGIN_LOCATION = "lastLoginLocation";
        public static final String REG_SOURCE = "regSource";
        public static final String I_RANK = "irank";
        public static final String RBA = "rba";
        public static final String SUBSCRIPTIONS = "subscriptions";
        public static final String USER_INFO = "userInfo";
        public static final String PREFERENCES = "preferences";
        public static final String GROUPS = "groups";
    }

    public static class AccountProfileExtraFields {

        @Retention(RetentionPolicy.SOURCE)
        @StringDef({LANGUAGES, ADDRESS, PHONES, EDUCATION, EDUCATION_LEVEL, HONORS, PUBLICATIONS, PATENTS, CERTIFICATIONS,
                PROFESSIONAL_HEADLINE, BIO, INDUSTRY, SPECIALITIES, WORK, SKILLS, RELIGION, POLITICAL_VIEW, INTERESTED_IN,
                RELATIONSHIP_STATUS, HOMETOWN, FAVORITES, FOLLOWERS_COUNT, FOLLOWING_COUNT, USERNAME, NAME, LOCALE, VERIFIED,
                TIMEZONE, LIKES, SAMLDATA})
        public @interface ProfileExtraFields {

        }

        public static final String LANGUAGES = "languages";
        public static final String ADDRESS = "address";
        public static final String PHONES = "phones";
        public static final String EDUCATION = "education";
        public static final String EDUCATION_LEVEL = "educationLevel";
        public static final String HONORS = "honors";
        public static final String PUBLICATIONS = "publications";
        public static final String PATENTS = "patents";
        public static final String CERTIFICATIONS = "certifications";
        public static final String PROFESSIONAL_HEADLINE = "professionalHeadline";
        public static final String BIO = "bio";
        public static final String INDUSTRY = "industry";
        public static final String SPECIALITIES = "specialties";
        public static final String WORK = "work";
        public static final String SKILLS = "skills";
        public static final String RELIGION = "religion";
        public static final String POLITICAL_VIEW = "politicalView";
        public static final String INTERESTED_IN = "interestedIn";
        public static final String RELATIONSHIP_STATUS = "relationshipStatus";
        public static final String HOMETOWN = "hometown";
        public static final String FAVORITES = "favorites";
        public static final String FOLLOWERS_COUNT = "followersCount";
        public static final String FOLLOWING_COUNT = "followingCount";
        public static final String USERNAME = "username";
        public static final String NAME = "name";
        public static final String LOCALE = "locale";
        public static final String VERIFIED = "verified";
        public static final String TIMEZONE = "timezone";
        public static final String LIKES = "likes";
        public static final String SAMLDATA = "samlData";
    }

}
