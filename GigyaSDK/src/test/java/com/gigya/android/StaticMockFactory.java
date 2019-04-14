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
}
