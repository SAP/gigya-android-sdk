package com.gigya.android.sdk.tfa;

public class GigyaDefinitions {

    /**
     * Local broadcast specific constants.
     */
    public static class Broadcasts {
        // ACTIONS
        public static final String INTENT_ACTION_TFA_APPROVE = "com.gigya.android.sdk.tfa.push_approve";
        public static final String INTENT_ACTION_TFA_DENY = "com.gigya.android.sdk.tfa.push_deny";
    }

    public static class Codes {
        public static final int PUST_TFA_CONTENT_ACTION_REQUEST_CODE = 2020;
    }

}
