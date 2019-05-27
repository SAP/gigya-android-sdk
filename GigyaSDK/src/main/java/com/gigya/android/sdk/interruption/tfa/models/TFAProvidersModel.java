package com.gigya.android.sdk.interruption.tfa.models;

import com.gigya.android.sdk.network.GigyaResponseModel;

import java.util.ArrayList;
import java.util.List;

public class TFAProvidersModel extends GigyaResponseModel {

    private List<TFAProvider> activeProviders = new ArrayList<>();
    private List<TFAProvider> inactiveProviders = new ArrayList<>();

    public List<TFAProvider> getActiveProviders() {
        return activeProviders;
    }

    public List<TFAProvider> getInactiveProviders() {
        return inactiveProviders;
    }
}
