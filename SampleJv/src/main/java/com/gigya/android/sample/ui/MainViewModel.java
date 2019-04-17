package com.gigya.android.sample.ui;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.support.annotation.NonNull;

import com.gigya.android.sample.model.MyAccount;
import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.network.GigyaError;

import java.util.HashMap;

import static com.gigya.android.sdk.GigyaDefinitions.Providers.FACEBOOK;

public class MainViewModel extends AndroidViewModel {

    public MainViewModel(@NonNull Application application) {
        super(application);
    }

    /*
    Referencing the Gigya interface. Assuming it has already been initialized in the main
    Application class.
     */
    private Gigya<MyAccount> mGigya = Gigya.getInstance(getApplication(), MyAccount.class);

    void test() {

        mGigya.login(FACEBOOK, new HashMap<>(), new GigyaLoginCallback<MyAccount>() {
            @Override
            public void onSuccess(MyAccount obj) {
                // Success
            }

            @Override
            public void onError(GigyaError error) {
                // Fail
            }
        });
    }
}
