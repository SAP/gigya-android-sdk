package com.gigya.android.sdk.ui.screensets;

import com.gigya.android.sdk.ApiManager;
import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.PersistenceManager;
import com.gigya.android.sdk.ui.GigyaPresenter;

import java.util.Map;

public class GigyaScreenSetsPresenter extends GigyaPresenter {
    GigyaScreenSetsPresenter(ApiManager apiManager, PersistenceManager persistenceManager) {
        super(apiManager, persistenceManager);
    }

    public <T> void showScreenSets(Map<String, Object> params, final GigyaCallback<T> callback) {

    }
}
