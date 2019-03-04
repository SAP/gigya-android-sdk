package com.gigya.android.sdk.model.tfa;

import com.gigya.android.sdk.model.BaseGigyaResponse;

import java.util.ArrayList;
import java.util.List;

public class TFAGetEmailsResponse extends BaseGigyaResponse {

    private List<TFAEmail> emails = new ArrayList<>();

    public List<TFAEmail> getEmails() {
        return emails;
    }
}
