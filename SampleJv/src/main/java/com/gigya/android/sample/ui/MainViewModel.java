package com.gigya.android.sample.ui;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;

import com.gigya.android.sample.model.MyAccount;
import com.gigya.android.sdk.Gigya;

public class MainViewModel extends AndroidViewModel {

    public MainViewModel(@NonNull Application application) {
        super(application);
    }

    private MutableLiveData<MyAccount> myAccontLiveData;

    /*
    Referencing the Gigya interface. Assuming it has already been initialized in the main
    Application class.
     */
    private Gigya<MyAccount> mGigya = Gigya.getInstance(getApplication(), MyAccount.class);

    boolean isLoggedIn() {
        return mGigya.isLoggedIn();
    }

}
