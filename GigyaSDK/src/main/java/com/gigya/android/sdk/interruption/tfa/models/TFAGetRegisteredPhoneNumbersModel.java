package com.gigya.android.sdk.interruption.tfa.models;

import com.gigya.android.sdk.network.GigyaResponseModel;

import java.util.ArrayList;
import java.util.List;

public class TFAGetRegisteredPhoneNumbersModel extends GigyaResponseModel {

    private List<TFARegisteredPhone> phones = new ArrayList<>();

    public List<TFARegisteredPhone> getPhones() {
        return phones;
    }
}
