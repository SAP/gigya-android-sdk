package com.gigya.android.gigyademo.ui.activity;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.gigya.android.gigyademo.R;
import com.gigya.android.gigyademo.model.CustomAccount;
import com.gigya.android.gigyademo.model.DataEvent;
import com.gigya.android.gigyademo.model.ErrorEvent;
import com.squareup.picasso.Picasso;

public class AccountActivity extends AbstractActivity {

    private AccountViewModel mViewModel;

    // Ui references.
    private ImageView mAccountImageView;
    private TextView mAccountNameTextView, mAccountEmailTextView;
    private Button mLogoutButton, mRegisterForPushAuthButton;
    private ProgressBar mAccountProgress;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        mViewModel = ViewModelProviders.of(this).get(AccountViewModel.class);
        mViewModel.getUpdatedAccountInformation();
        mViewModel.registerForRemoteNotifications(this);

        evaluateSessionStateUI();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mViewModel.mDataRouter.observe(this, accountObserver);
        mViewModel.mErrorRouter.observe(this, errorObserver);
    }

    @Override
    protected void onStop() {
        mViewModel.mDataRouter.removeObserver(accountObserver);
        mViewModel.mErrorRouter.removeObserver(errorObserver);
        super.onStop();
    }

    /*
    Data observer.
     */
    final Observer<DataEvent> accountObserver = event -> {
        if (event == null) {
            return;
        }
        if (event.isObserved()) {
            return;
        }
        switch (event.getAction()) {
            case DataEvent.ROUTE_AUTH_DEVICE_REGISTER:
                updateLoginProgress(false);
                centerToastWith("Device successfully registered for push message authentication service");
                break;
            case DataEvent.ROUTE_GET_ACCOUNT_INFO:
                updateLoginProgress(false);
                final CustomAccount customAccount = (CustomAccount) event.getData();
                if (customAccount.getProfile() == null) {
                    return;
                }

                // Display account image.
                if (mAccountImageView == null) {
                    mAccountImageView = findViewById(R.id.account_image_view);
                }
                Picasso.get().load(customAccount.getProfile().getPhotoURL()).into(mAccountImageView);

                // Display account name.
                if (mAccountNameTextView == null) {
                    mAccountNameTextView = findViewById(R.id.account_name_text);
                }
                final String firstName = customAccount.getProfile().getFirstName();
                final String lastName = customAccount.getProfile().getLastName();
                final String accountFullName = String.format(
                        "%s %s",
                        TextUtils.isEmpty(firstName) ? "" : firstName,
                        TextUtils.isEmpty(lastName) ? "" : lastName
                );
                mAccountNameTextView.setText(accountFullName);

                // Display account email.
                if (mAccountEmailTextView == null) {
                    mAccountEmailTextView = findViewById(R.id.account_email_text);
                }
                final String accountEmail = customAccount.getProfile().getEmail();
                mAccountEmailTextView.setText(accountEmail);

                centerToastWith("Account successfully loaded");
                break;
        }
        event.setObserved(true);
    };

    /*
    Error observer.
     */
    final Observer<ErrorEvent> errorObserver = event -> {
        updateLoginProgress(false);
        if (event.isObserved()) {
            return;
        }
        Log.e("AccountActivity", event.getError().getLocalizedMessage());
        centerToastWith("Login error occurred. Quick call Gigya support!!!!!");
        event.setObserved(true);
    };

    /**
     * Evaluate current session state to update UI elements accordingly.
     */
    private void evaluateSessionStateUI() {
        if (mLogoutButton == null) {
            mLogoutButton = findViewById(R.id.sign_out_button);
        }
        if (mRegisterForPushAuthButton == null) {
            mRegisterForPushAuthButton = findViewById(R.id.register_device_button);
        }
        mLogoutButton.setEnabled(mViewModel.isLoggedIn());
        mRegisterForPushAuthButton.setEnabled(mViewModel.isLoggedIn());
    }

    /*
    Update login progress bar state according to... wait for it.......... login progress state.
    */
    private void updateLoginProgress(boolean visible) {
        if (mAccountProgress == null) {
            mAccountProgress = findViewById(R.id.account_progress);
        }
        mAccountProgress.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /**
     * Call to refresh account information.
     */
    public void onAccountRefreshClick(View clickView) {
        updateLoginProgress(true);
        mViewModel.getUpdatedAccountInformation();
    }

    /**
     * Show account information secreenset.
     *
     * @param clickView
     */
    public void onAccountInfoScreenSetClick(View clickView) {
        mViewModel.showAccountInfoScreenSet();
    }

    /**
     * Logout of current active session.
     */
    public void onLogoutClick(View clickView) {
        mViewModel.logoutOfGigyaAccount();
        evaluateSessionStateUI();

        // Start the login activity again.
        startActivity(new Intent(this, LoginActivity.class));
    }

    public void onRegisterForPushAuth(final View clickView) {
        updateLoginProgress(true);
        mViewModel.registerForPushAuthentication();
    }
}
