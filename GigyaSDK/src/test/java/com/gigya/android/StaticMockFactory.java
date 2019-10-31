package com.gigya.android;

public class StaticMockFactory {

    public static final String API_KEY = "3_eP-lTMvtVwgjBCKCWPgYfeWH4xVkD5Rga15I7aoVvo-S_J5ZRBLg9jLDgJvDJZag";
    public static final String API_DOMAIN = "us1-gigya.com";
    public static final String UCID = "qweqweasd123456123";
    public static final String GMID = "asdqwezxc123asd123";
    public static final String TOKEN = "st2.sfoajsf923rqn1fasijasd213sddajaASDFajkag34fQEGAJSFG09g09jgasdg";
    public static final String SECRET = "fajosdsas3431ASFasfkt32";

    public static String getMockSecret() {
        return SECRET;
    }

    public static String getMockToken() {
        return TOKEN;
    }

    public static String getMockConfigurationFileJson() {
        return "{\n" +
                "  \"apiKey\": \"3_l7zxNcj4vhu8tLzYafUnKDSA4VsOVNzR4VnclcC6VKsXXmQdq950uC-zY7Vsu9RC\",\n" +
                "  \"apiDomain\": \"us1.gigya.com\",\n" +
                "  \"accountCacheTime\": 1,\n" +
                "  \"sessionVerificationInterval\": 0\n" +
                "}";
    }

    public static String getSessionMock() {
        return "{\n" +
                "  \"sessionToken\": \"" + TOKEN + "\",\n" +
                "  \"sessionSecret\": \"" + SECRET + "\",\n" +
                "  \"expirationTime\": 0\n" +
                "}";
    }

    public static String getSessionMockWithFiveMinutesExpiration() {
        return "{\n" +
                "  \"sessionToken\": \"" + TOKEN + "\",\n" +
                "  \"sessionSecret\": \"" + SECRET + "\",\n" +
                "  \"expirationTime\": 5\n" +
                "}";
    }

    public static String getLegacySessionMock() {
        return "{\n" +
                "  \"session.Token\": \"" + TOKEN + "\",\n" +
                "  \"session.Secret\": \"" + SECRET + "\",\n" +
                "  \"session.ExpirationTime\": 0\n" +
                "}";
    }

    public static String getMockResponseJson() {
        return "{\n" +
                "  \"callId\": \"d6e963d1bf5c4d73a010b06fe2182f6c\",\n" +
                "  \"errorCode\": 0,\n" +
                "  \"apiVersion\": 2,\n" +
                "  \"statusCode\": 200,\n" +
                "  \"statusReason\": \"OK\",\n" +
                "  \"time\": \"2019-06-02T06:42:55.678Z\"\n" +
                "}";
    }

    public static String getMockGigyaErorJson() {
        return "{\n" +
                "  \"callId\": \"d6e963d1bf5c4d73a010b06fe2182f6c\",\n" +
                "  \"errorCode\": 400001,\n" +
                "  \"apiVersion\": 2,\n" +
                "  \"statusCode\": 200,\n" +
                "  \"statusReason\": \"OK\",\n" +
                "  \"time\": \"2019-06-02T06:42:55.678Z\"\n" +
                "}";
    }

    public static String getMockConfigJson() {
        return "{\n" +
                "\t\"errorReportRules\": [],\n" +
                "\t\"permissions\": {\n" +
                "\t\t\"facebook\": [\n" +
                "\t\t\t\"user_friends\"],\n" +
                "\t\t\"googleplus\": [\n" +
                "\t\t\t\"profile\",\n" +
                "\t\t\t\"email\",\n" +
                "\t\t\t\"openid\"]\n" +
                "\t},\n" +
                "\t\"appIds\": {\n" +
                "\t\t\"googleplus\": \"977811956095-t72doari7i8iuv9r6qf3kbhh2ns3u7cj.apps.googleusercontent.com\"\n" +
                "\t},\n" +
                "\t\"ids\": {\n" +
                "\t\t\"gmid\": \"KoRxXCZzFKoAFl2jL2WuJMZV4H0nx9NJJ7jxmgJyA7c=\",\n" +
                "\t\t\"ucid\": \"ff3f112d92b657ee\"\n" +
                "\t},\n" +
                "\t\"statusCode\": 200,\n" +
                "\t\"errorCode\": 0,\n" +
                "\t\"statusReason\": \"OK\",\n" +
                "\t\"callId\": \"bd22e624bea844b8827b43e1587711fe\",\n" +
                "\t\"time\": \"2019-06-02T11:03:57.415Z\"\n" +
                "}";
    }

    public static String getMockAccountJson() {
        return "{\n" +
                "\t\"callId\": \"fd30ce9173d24d0d9edce38f7c711b51\",\n" +
                "\t\"errorCode\": 0,\n" +
                "\t\"apiVersion\": 2,\n" +
                "\t\"statusCode\": 200,\n" +
                "\t\"statusReason\": \"OK\",\n" +
                "\t\"time\": \"2019-04-11T14:30:13.724Z\",\n" +
                "\t\"registeredTimestamp\": 1554991351000,\n" +
                "\t\"UID\": \"06529a7a82e2478a8b008a08dafcf20f\",\n" +
                "\t\"UIDSignature\": \"zqcan3QTdNC9GhUSJGa7pWKcdV4=\",\n" +
                "\t\"signatureTimestamp\": \"1554993013\",\n" +
                "\t\"created\": \"2019-04-11T14:02:31.729Z\",\n" +
                "\t\"createdTimestamp\": 1554991351000,\n" +
                "\t\"data\": {},\n" +
                "\t\"subscriptions\": {},\n" +
                "\t\"preferences\": {},\n" +
                "\t\"emails\": {\n" +
                "\t\t\"verified\": [\n" +
                "\t\t\t\"toolmarmel@gmail.com\"],\n" +
                "\t\t\"unverified\": []\n" +
                "\t},\n" +
                "\t\"isActive\": true,\n" +
                "\t\"isRegistered\": true,\n" +
                "\t\"isVerified\": true,\n" +
                "\t\"lastLogin\": \"2019-04-11T14:03:36.463Z\",\n" +
                "\t\"lastLoginTimestamp\": 1554991416000,\n" +
                "\t\"lastUpdated\": \"2019-04-11T14:03:36.390Z\",\n" +
                "\t\"lastUpdatedTimestamp\": 1554991416390,\n" +
                "\t\"loginProvider\": \"facebook\",\n" +
                "\t\"oldestDataUpdated\": \"2019-04-11T14:02:31.729Z\",\n" +
                "\t\"oldestDataUpdatedTimestamp\": 1554991351729,\n" +
                "\t\"profile\": {\n" +
                "\t\t\"firstName\": \"Tal\",\n" +
                "\t\t\"lastName\": \"Mirmelshtein\",\n" +
                "\t\t\"email\": \"toolmarmel@gmail.com\",\n" +
                "\t\t\"gender\": \"u\",\n" +
                "\t\t\"photoURL\": \"https://graph.facebook.com/v2.12/10156771694254594/picture?type=large\",\n" +
                "\t\t\"thumbnailURL\": \"https://graph.facebook.com/v2.12/10156771694254594/picture?type=square\"\n" +
                "\t},\n" +
                "\t\"registered\": \"2019-04-11T14:02:31.990Z\",\n" +
                "\t\"socialProviders\": \"facebook\",\n" +
                "\t\"verified\": \"2019-04-11T14:02:31.939Z\",\n" +
                "\t\"verifiedTimestamp\": 1554991351939\n" +
                "}";
    }

    public static String getMockLogoutJson() {
        return "{\n" +
                "\t\"connectedProviders\": \"site\",\n" +
                "\t\"UID\": \"0e5712b22b5a42e8be28747b4c076214\",\n" +
                "\t\"logoutActiveSession\": true,\n" +
                "\t\"callId\": \"511f334d792c4958bc555cd71f78088e\",\n" +
                "\t\"errorCode\": 0,\n" +
                "\t\"apiVersion\": 2,\n" +
                "\t\"statusCode\": 200,\n" +
                "\t\"statusReason\": \"OK\",\n" +
                "\t\"time\": \"2019-06-02T11:05:02.009Z\"\n" +
                "}";
    }

    public static String getMockSetAccountJson() {
        return "{\n" +
                "\t\"callId\": \"c11778cdb52046efa4c416383454ebc7\",\n" +
                "\t\"errorCode\": 0,\n" +
                "\t\"apiVersion\": 2,\n" +
                "\t\"statusCode\": 200,\n" +
                "\t\"statusReason\": \"OK\",\n" +
                "\t\"time\": \"2019-06-04T11:23:46.399Z\"\n" +
                "}";
    }

    public static String mockRequestExpiredErrorJson() {
        return "{\n" +
                "  \"callId\": \"93c52cdd21c243559a00f5fa4c798742\",\n" +
                "  \"errorCode\": 403002,\n" +
                "  \"errorDetails\": \"The requests timestamp skew more than 120 seconds from the server time. Please check you server's time and timezone configuration. Server timestamp: 1572514484, Request timestamp: 1572515113\",\n" +
                "  \"errorMessage\": \"Request has expired\",\n" +
                "  \"apiVersion\": 2,\n" +
                "  \"statusCode\": 403,\n" +
                "  \"statusReason\": \"Forbidden\",\n" +
                "  \"time\": \"2019-10-31T09:34:44.285Z\"\n" +
                "}\n";
    }
}
