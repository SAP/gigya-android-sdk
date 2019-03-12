package com.gigya.android.sdk.model.account;

import java.util.ArrayList;
import java.util.List;

public class Emails {

    private List<String> unverified = new ArrayList<>();
    private List<String> verified = new ArrayList<>();

    public List<String> getUnverified() {
        return unverified;
    }

    public void setUnverified(List<String> unverified) {
        this.unverified = unverified;
    }

    public List<String> getVerified() {
        return verified;
    }

    public void setVerified(List<String> verified) {
        this.verified = verified;
    }
}
