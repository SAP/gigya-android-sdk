package com.gigya.android.sdk.model.tfa;

import com.gigya.android.sdk.model.GigyaResponseModel;

import java.util.ArrayList;
import java.util.List;

public class TFAGetEmailsModel extends GigyaResponseModel {

    private List<TFAEmail> emails = new ArrayList<>();

    public List<TFAEmail> getEmails() {
        return emails;
    }
}
