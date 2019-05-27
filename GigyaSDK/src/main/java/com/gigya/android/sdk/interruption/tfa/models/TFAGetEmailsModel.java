package com.gigya.android.sdk.interruption.tfa.models;

import com.gigya.android.sdk.network.GigyaResponseModel;

import java.util.ArrayList;
import java.util.List;

public class TFAGetEmailsModel extends GigyaResponseModel {

    private List<TFAEmail> emails = new ArrayList<>();

    public List<TFAEmail> getEmails() {
        return emails;
    }
}
