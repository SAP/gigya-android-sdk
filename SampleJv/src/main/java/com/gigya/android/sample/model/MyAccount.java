package com.gigya.android.sample.model;

import com.gigya.android.sdk.model.account.GigyaAccount;

public class MyAccount extends GigyaAccount {

    private MyData data;

    public MyData getData() {
        return data;
    }

    private static class MyData {

        private String comment;
        private Boolean subscribe;
        private Boolean terms;

        public String getComment() {
            return comment;
        }

        public Boolean getSubscribe() {
            return subscribe;
        }

        public Boolean getTerms() {
            return terms;
        }
    }
}
