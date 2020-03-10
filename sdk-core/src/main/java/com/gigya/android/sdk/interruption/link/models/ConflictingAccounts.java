package com.gigya.android.sdk.interruption.link.models;

import com.gigya.android.sdk.network.GigyaResponseModel;

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
