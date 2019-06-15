package com.gigya.android.sdk.tfa.resolvers.models;

import com.gigya.android.sdk.network.GigyaResponseModel;

import java.util.ArrayList;
import java.util.List;

public class ProvidersModel extends GigyaResponseModel {

    private List<Provider> activeProviders = new ArrayList<>();
    private List<Provider> inactiveProviders = new ArrayList<>();

    public List<Provider> getActiveProviders() {
        return activeProviders;
    }

    public List<Provider> getInactiveProviders() {
        return inactiveProviders;
    }
}
