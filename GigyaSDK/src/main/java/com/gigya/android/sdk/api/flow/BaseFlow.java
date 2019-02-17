package com.gigya.android.sdk.api.flow;

import com.gigya.android.sdk.AccountManager;
import com.gigya.android.sdk.DependencyRegistry;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;

public class BaseFlow {

    private Configuration configuration;
    private NetworkAdapter networkAdapter;
    private AccountManager accountManager;
    private SessionManager sessionManager;

    public void inject(Configuration configuration, NetworkAdapter networkAdapter,
                       AccountManager accountManager, SessionManager sessionManager) {
        this.configuration = configuration;
        this.networkAdapter = networkAdapter;
        this.accountManager = accountManager;
        this.sessionManager = sessionManager;
    }

    BaseFlow() {
        DependencyRegistry.getInstance().inject(this);
    }
}
