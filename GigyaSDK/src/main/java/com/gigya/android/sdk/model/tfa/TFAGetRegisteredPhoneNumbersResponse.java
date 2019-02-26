package com.gigya.android.sdk.model.tfa;

import com.gigya.android.sdk.model.BaseGigyaResponse;

import java.util.ArrayList;
import java.util.List;

public class TFAGetRegisteredPhoneNumbersResponse extends BaseGigyaResponse {

    private List<TFARegisteredPhone> phones = new ArrayList<>();

    public List<TFARegisteredPhone> getPhones() {
        return phones;
    }
}
