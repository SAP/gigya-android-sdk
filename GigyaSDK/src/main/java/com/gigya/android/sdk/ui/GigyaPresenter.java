package com.gigya.android.sdk.ui;

import android.util.SparseArray;

import com.gigya.android.sdk.AccountManager;
import com.gigya.android.sdk.DependencyRegistry;
import com.gigya.android.sdk.SessionManager;

public abstract class GigyaPresenter {

    private static final String LOG_TAG = "GigyaPresenter";

    public static final String SHOW_FULL_SCREEN = "show_full_screen";
    public static final String PROGRESS_COLOR = "progress_color";

    protected SessionManager _sessionManager;
    protected AccountManager _accountManager;

    public GigyaPresenter() {
        DependencyRegistry.getInstance().inject(this);
    }


    public void inject(SessionManager sessionManager, AccountManager accountManager) {
        _sessionManager = sessionManager;
        _accountManager = accountManager;
    }

    //region HostActivity lifecycle callbacks tracking

    // TODO: 03/01/2019 When dropping support for <17 devices remove static references!!! Use Binder instead to attach the callbacks to the activity intent.

    private static SparseArray<HostActivity.HostActivityLifecycleCallbacks> lifecycleSparse = new SparseArray<>();

    public static int addLifecycleCallbacks(HostActivity.HostActivityLifecycleCallbacks callbacks) {
        int id = callbacks.hashCode();
        lifecycleSparse.append(id, callbacks);
        return id;
    }

    public static HostActivity.HostActivityLifecycleCallbacks getCallbacks(int id) {
        return lifecycleSparse.get(id);
    }

    public static void flushLifecycleCallbacks(int id) {
        lifecycleSparse.remove(id);
    }

    public static void flush() {
        lifecycleSparse.clear();
        System.gc();
    }

    //endregion
}
