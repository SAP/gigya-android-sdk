package com.gigya.android.sdk.ui;

import android.util.SparseArray;

import com.gigya.android.sdk.GigyaContext;
import com.gigya.android.sdk.services.AccountService;
import com.gigya.android.sdk.services.Config;
import com.gigya.android.sdk.services.SessionService;

public abstract class GigyaPresenter {

    public static final String SHOW_FULL_SCREEN = "style_show_full_screen";
    public static final String PROGRESS_COLOR = "style_progress_color";
    public static final String CORNER_RADIUS = "style_corner_radius";
    public static final String DIALOG_MAX_WIDTH = "dialog_max_width";
    public static final String DIALOG_MAX_HEIGHT = "dialog_max_height";

    protected Config _config;
    protected SessionService _sessionService;
    protected AccountService _accountService;

    public GigyaPresenter(GigyaContext gigyaContext) {
        _config = gigyaContext.getConfig();
        _sessionService = gigyaContext.getSessionService();
        _accountService = gigyaContext.getAccountService();
    }

    //region HOSTACTIVITY LIFECYCLE CALLBACKS TRACKING

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
