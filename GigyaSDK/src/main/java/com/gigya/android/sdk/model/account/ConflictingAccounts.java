package com.gigya.android.sdk.model.account;

import com.gigya.android.sdk.model.GigyaResponseModel;

import java.util.ArrayList;

public class ConflictingAccounts extends GigyaResponseModel {

    private ArrayList<String> loginProviders = new ArrayList<>();
    private String loginID;

    public ArrayList<String> getLoginProviders() {
        return loginProviders;
    }

    public String getLoginID() {
        return loginID;
    }
}
