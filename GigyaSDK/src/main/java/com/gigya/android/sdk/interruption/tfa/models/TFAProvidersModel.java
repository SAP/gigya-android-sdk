package com.gigya.android.sdk.interruption.tfa.models;

import com.gigya.android.sdk.network.GigyaResponseModel;

import java.util.ArrayList;
import java.util.List;

public class TFAProvidersModel extends GigyaResponseModel {

    private List<TFAProviderModel> activeProviders = new ArrayList<>();
    private List<TFAProviderModel> inactiveProviders = new ArrayList<>();

    public List<TFAProviderModel> getActiveProviders() {
        return activeProviders;
    }

    public List<TFAProviderModel> getInactiveProviders() {
        return inactiveProviders;
    }
}
