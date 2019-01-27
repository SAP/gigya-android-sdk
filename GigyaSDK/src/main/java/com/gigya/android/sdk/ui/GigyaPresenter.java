package com.gigya.android.sdk.ui;

import android.util.SparseArray;

import com.gigya.android.sdk.ApiManager;
import com.gigya.android.sdk.PersistenceManager;

public abstract class GigyaPresenter {

    protected ApiManager _apiManager;
    protected PersistenceManager _persistenceManager;

    public GigyaPresenter(ApiManager apiManager, PersistenceManager persistenceManager) {
        _apiManager = apiManager;
        _persistenceManager = persistenceManager;
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
