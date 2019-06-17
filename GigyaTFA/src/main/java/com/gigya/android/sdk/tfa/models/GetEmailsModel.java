package com.gigya.android.sdk.tfa.models;

import com.gigya.android.sdk.network.GigyaResponseModel;

import java.util.ArrayList;
import java.util.List;

public class GetEmailsModel extends GigyaResponseModel {

    private List<EmailModel> emails = new ArrayList<>();

    public List<EmailModel> getEmails() {
        return emails;
    }
}
