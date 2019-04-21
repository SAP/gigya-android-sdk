package com.gigya.android.sample.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;

import com.gigya.android.sample.R;
import com.gigya.android.sdk.GigyaDefinitions;

public class MainActivity extends AppCompatActivity {

    private MainViewModel mViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTitle("Gigya SDK playground");
        mViewModel = ViewModelProviders.of(this).get(MainViewModel.class);
    }

    /**
     * Session handling broadcast receiver.
     */
    private BroadcastReceiver mSessionLifecycleReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action == null) {
                return;
            }
            switch (action) {
                case GigyaDefinitions.Broadcasts.INTENT_ACTION_SESSION_EXPIRED:
                    // Session has expired -> SDK session expired event (registration with session expiration).
                    break;
                case GigyaDefinitions.Broadcasts.INTENT_ACTION_SESSION_INVALID:
                    // Session is invalid -> SDK session validation interval event.
                    break;
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(GigyaDefinitions.Broadcasts.INTENT_ACTION_SESSION_EXPIRED);
        intentFilter.addAction(GigyaDefinitions.Broadcasts.INTENT_ACTION_SESSION_INVALID);
        LocalBroadcastManager.getInstance(this).registerReceiver(mSessionLifecycleReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mSessionLifecycleReceiver);
        super.onPause();
    }
}
