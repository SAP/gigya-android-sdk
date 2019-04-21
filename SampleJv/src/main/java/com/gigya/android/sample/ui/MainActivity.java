package com.gigya.android.sample.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.gigya.android.sample.R;
import com.gigya.android.sdk.GigyaDefinitions;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private MainViewModel mViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTitle("Gigya SDK sample");
        mViewModel = ViewModelProviders.of(this).get(MainViewModel.class);

        initDrawer();
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
                    break;
                case GigyaDefinitions.Broadcasts.INTENT_ACTION_SESSION_INVALID:
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

    private void initDrawer() {
        final DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        final Toolbar toolbar = findViewById(R.id.toolbar);
        final NavigationView navigationView = findViewById(R.id.nav_view);
        final ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);
        navigationView.getHeaderView(0).setOnClickListener(v -> {
            if (mViewModel.isLoggedIn()) {
                // Show account info.
            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.api_anonymous:
                break;
            case R.id.api_login:
                break;
            case R.id.api_login_with_provider:
                break;
            case R.id.api_add_connection:
                break;
            case R.id.api_remove_connection:
                break;
            case R.id.api_register:
                break;
            case R.id.api_get_account_info:
                break;
            case R.id.api_set_account_info:
                break;
            case R.id.api_verify_login:
                break;
            case R.id.action_raas:
                break;
            case R.id.action_native_login:
                break;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        final MenuItem accountItem = menu.findItem(R.id.action_account);
        final MenuItem logoutItem = menu.findItem(R.id.action_logout);

        accountItem.setVisible(mViewModel.isLoggedIn());
        logoutItem.setVisible(mViewModel.isLoggedIn());
        
       return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_account:
                break;
            case R.id.action_clear:
                break;
            case R.id.action_reinit:
                break;
            case R.id.action_logout:
                break;
            case R.id.toggle_interruptions:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
