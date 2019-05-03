package com.gigya.android.sdk.model.tfa;

import com.gigya.android.sdk.model.GigyaResponseModel;

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
