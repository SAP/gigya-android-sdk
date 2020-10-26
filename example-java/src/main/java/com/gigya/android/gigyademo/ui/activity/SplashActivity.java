package com.gigya.android.gigyademo.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


import com.gigya.android.gigyademo.R;
import com.gigya.android.gigyademo.model.CustomAccount;
import com.gigya.android.sdk.Gigya;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        /*
        Using a short delay just to give a little feel of splash.
         */
        new Handler().postDelayed(() -> {
            final boolean isLoggedIn = Gigya.getInstance(CustomAccount.class).isLoggedIn();
            if (isLoggedIn) {
                startActivity(new Intent(SplashActivity.this, AccountActivity.class));
            } else {
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            }
            finish();
        }, 1500);

    }
}
