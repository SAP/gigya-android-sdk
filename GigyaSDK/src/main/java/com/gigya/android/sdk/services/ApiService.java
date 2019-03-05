package com.gigya.android.sdk.services;

import android.content.Context;

import com.gigya.android.sdk.model.account.GigyaAccount;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;

public class ApiService<T extends GigyaAccount> {

    private static final String LOG_TAG = "ApiService";

    /*
    Network adapter provided for handling HTTP requests & results.
     */
    final private NetworkAdapter _adapter;

    /*
    Required services for API integrity and logic.
     */
    final private SessionService _sessionService;
    final private AccountService<T> _accountService;

    public ApiService(Context appContext, SessionService sessionService, AccountService<T> accountService) {
        _sessionService = sessionService;
        _accountService = accountService;
        _adapter = new NetworkAdapter(appContext, new NetworkAdapter.IConfigurationBlock() {
            @Override
            public void onMissingConfiguration() {
                if (_sessionService.getConfig().getGmid() == null) {
                    // Fetch new config.
                }
            }
        });
    }

    //region Available APIs

    public void loadConfig() {

    }

    public void send() {

    }

    public void logout() {

    }

    public void login() {

    }

    public void getAccount() {

    }

    public void setAccount() {

    }

    public void register() {

    }

    public void forgotPassword() {

    }

    public void nativeLogin() {

    }

    public void refreshNativeProvicerSession() {

    }

    //endregion

}
