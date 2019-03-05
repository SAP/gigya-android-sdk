package com.gigya.android.sdk.model.tfa;

import com.gigya.android.sdk.model.GigyaModel;

import java.util.ArrayList;
import java.util.List;

public class TFAGetRegisteredPhoneNumbersModel extends GigyaModel {

    private List<TFARegisteredPhone> phones = new ArrayList<>();

    public List<TFARegisteredPhone> getPhones() {
        return phones;
    }
}
