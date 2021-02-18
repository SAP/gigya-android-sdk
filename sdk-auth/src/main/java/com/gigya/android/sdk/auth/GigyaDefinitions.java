package com.gigya.android.sdk.auth;

public class GigyaDefinitions {

    public static final int PUSH_AUTH_CONTENT_ACTION_REQUEST_CODE = 3020;
    public static final int PUSH_AUTH_CONTENT_INTENT_REQUEST_CODE = 3021;

    public static final String AUTH_CHANNEL_ID = "auth_channel";

    public static class API {

        // Push
        public static final String API_AUTH_DEVICE_REGISTER = "accounts.devices.register";
        public static final String API_AUTH_DEVICE_UNREGISTER = "accounts.devices.unregister";
        public static final String API_AUTH_PUSH_VERIFY = "accounts.auth.push.verify";

        // OTP
        public static final String API_AUTH_OTP_SEND_CODE = "accounts.otp.sendCode";
        public static final String API_AUTH_OTP_LOGIN = "accounts.otp.login";
        public static final String API_AUTH_OTP_UPDATE = "accounts.otp.update";

    }

}
