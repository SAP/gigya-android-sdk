package com.gigya.android.gigyademo.ui.activity;

import androidx.appcompat.app.AppCompatActivity;
import android.view.Gravity;
import android.widget.Toast;

public abstract class AbstractActivity extends AppCompatActivity {

    public void centerToastWith(String message) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }
}
