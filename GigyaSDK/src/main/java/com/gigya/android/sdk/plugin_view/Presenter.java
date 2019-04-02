package com.gigya.android.sdk.plugin_view;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.gigya.android.sdk.ui.HostActivity;

public class Presenter implements IPresenter {

    final private IPluginFragmentFactory _pfgFactory;

    public Presenter(IPluginFragmentFactory pfgFactory) {
        _pfgFactory = pfgFactory;
    }

    @Override
    public void showPluginActivity(Context context, final boolean obfuscate) {
        HostActivity.present(context, new HostActivity.HostActivityLifecycleCallbacks() {

            @Override
            public void onCreate(AppCompatActivity activity, @Nullable Bundle savedInstanceState) {
                _pfgFactory.showFragment(obfuscate);
            }
        });
    }
}
