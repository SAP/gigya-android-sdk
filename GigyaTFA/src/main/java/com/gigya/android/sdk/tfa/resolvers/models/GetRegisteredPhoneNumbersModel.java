package com.gigya.android.sdk.tfa.resolvers.models;

import com.gigya.android.sdk.network.GigyaResponseModel;

import java.util.ArrayList;
import java.util.List;

public class GetRegisteredPhoneNumbersModel extends GigyaResponseModel {

    private List<RegisteredPhone> phones = new ArrayList<>();

    public List<RegisteredPhone> getPhones() {
        return phones;
    }
}
