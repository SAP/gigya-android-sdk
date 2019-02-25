package com.gigya.android.sdk.model.tfa;

import com.gigya.android.sdk.model.BaseGigyaResponse;

import java.util.ArrayList;
import java.util.List;

public class TFAProvidersResponse extends BaseGigyaResponse {

    private List<TFAProvider> activeProviders = new ArrayList<>();
    private List<TFAProvider> inactiveProviders = new ArrayList<>();

    public List<TFAProvider> getActiveProviders() {
        return activeProviders;
    }

    public List<TFAProvider> getInactiveProviders() {
        return inactiveProviders;
    }
}
