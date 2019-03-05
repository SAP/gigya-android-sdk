package com.gigya.android.sdk.model.tfa;

import com.gigya.android.sdk.model.GigyaModel;

import java.util.ArrayList;
import java.util.List;

public class TFAProvidersModel extends GigyaModel {

    private List<TFAProvider> activeProviders = new ArrayList<>();
    private List<TFAProvider> inactiveProviders = new ArrayList<>();

    public List<TFAProvider> getActiveProviders() {
        return activeProviders;
    }

    public List<TFAProvider> getInactiveProviders() {
        return inactiveProviders;
    }
}
