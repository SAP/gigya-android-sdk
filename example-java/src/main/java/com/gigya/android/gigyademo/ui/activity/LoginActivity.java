package com.gigya.android.gigyademo.ui.activity;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import androidx.core.util.Pair;
import androidx.appcompat.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.gigya.android.gigyademo.R;
import com.gigya.android.gigyademo.model.DataEvent;
import com.gigya.android.gigyademo.model.ErrorEvent;
import com.gigya.android.gigyademo.ui.sheets.DemoSiteChangeBottomSheet;
import com.gigya.android.gigyademo.ui.sheets.ForgotPasswordBottomSheet;
import com.gigya.android.gigyademo.ui.sheets.PendingRegistrationBottomSheet;
import com.gigya.android.gigyademo.ui.sheets.TFAPhoneRegistrationBottomSheet;
import com.gigya.android.gigyademo.ui.sheets.TFAPhoneVerificationBottomSheet;
import com.gigya.android.gigyademo.ui.sheets.TFAProviderSelectionBottomSheet;
import com.gigya.android.sdk.interruption.tfa.models.TFAProviderModel;
import com.gigya.android.sdk.tfa.ui.TFATOTPRegistrationFragment;
import com.gigya.android.sdk.tfa.ui.TFATOTPVerificationFragment;

import java.util.List;

import static com.gigya.android.sdk.GigyaDefinitions.Providers.FACEBOOK;
import static com.gigya.android.sdk.GigyaDefinitions.Providers.GOOGLE;

public class LoginActivity extends AbstractActivity {

    private LoginViewModel mViewModel;

    private EditText mUserName, mPassword;
    private ProgressBar mLoginProgress;
    private CheckBox mScreenSetsCheckbox;

    @Override
    protected void onStart() {
        super.onStart();
        mViewModel.mDataRouter.observe(this, dataRouteObserver);
        mViewModel.mErrorRouter.observe(this, errorObserver);
    }

    @Override
    protected void onStop() {
        mViewModel.mDataRouter.removeObserver(dataRouteObserver);
        mViewModel.mErrorRouter.removeObserver(errorObserver);
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mViewModel = ViewModelProviders.of(this).get(LoginViewModel.class);
        referenceUiElements();
    }

    private void referenceUiElements() {
        mUserName = findViewById(R.id.username_edit);
        mPassword = findViewById(R.id.password_edit);

        mScreenSetsCheckbox = findViewById(R.id.use_screensets_checkbox);
        mScreenSetsCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mUserName.setEnabled(!isChecked);
            mPassword.setEnabled(!isChecked);
        });
    }

    /*
    Data observer.
     */
    private Observer<DataEvent> dataRouteObserver = dataRoute -> {
        if (dataRoute == null) {
            return;
        }
        if (dataRoute.isObserved()) {
            return;
        }
        switch (dataRoute.getAction()) {
            case DataEvent.ROUTE_OPERATION_CANCELED:
                centerToastWith("Operation canceled");
                updateLoginProgress(false);
                break;
            case DataEvent.ROUTE_LOGIN_SUCCESS:
                updateLoginProgress(false);
                centerToastWith("Login Success! Woohoooo!!!!");
                startActivity(new Intent(this, AccountActivity.class));
                finish();
                break;
            case DataEvent.ROUTE_FORGOT_PASSWORD_EMAIL_SENT:
                centerToastWith("Email sent.");
                break;
            case DataEvent.ROUTE_PENDING_REGISTRATION:
                final PendingRegistrationBottomSheet pendingRegistrationBottomSheet = PendingRegistrationBottomSheet.newInstance();
                pendingRegistrationBottomSheet.show(getSupportFragmentManager(), PendingRegistrationBottomSheet.TAG);
                break;
            case DataEvent.ROUTE_TFA_PROVIDER_SELECTION:
                final Pair pair = (Pair) dataRoute.getData();
                final TFAProviderSelectionBottomSheet providerSelectionBottomSheet = TFAProviderSelectionBottomSheet
                        .newInstance(
                                (Integer) pair.first
                        );
                providerSelectionBottomSheet.setProviders((List<TFAProviderModel>) pair.second);
                providerSelectionBottomSheet.show(getSupportFragmentManager(), TFAProviderSelectionBottomSheet.TAG);
                break;
            case DataEvent.ROUTE_TFA_REGISTER_PHONE:
                final TFAPhoneRegistrationBottomSheet phoneRegistrationBottomSheet = TFAPhoneRegistrationBottomSheet.newInstance();
                phoneRegistrationBottomSheet.show(getSupportFragmentManager(), TFAPhoneRegistrationBottomSheet.TAG);
                break;
            case DataEvent.ROUTE_TFA_VERIFY_PHONE:
                final TFAPhoneVerificationBottomSheet phoneVerificationBottomSheet = TFAPhoneVerificationBottomSheet.newInstance();
                phoneVerificationBottomSheet.show(getSupportFragmentManager(), TFAPhoneVerificationBottomSheet.TAG);
                break;
            case DataEvent.ROUTE_TFA_REGISTER_TOTP:
                /*
                Using provided TFA user interface. A selection callback is available using "setSelectionCallback" method for
                additional flow control.
                 */
                TFATOTPRegistrationFragment tfatotpRegistrationFragment = TFATOTPRegistrationFragment.newInstance();
                tfatotpRegistrationFragment.setResolverFactory(mViewModel.getTFAResolverFactory());
                tfatotpRegistrationFragment.show(getSupportFragmentManager(), "TFATOTPRegistrationFragment");
                break;
            case DataEvent.ROUTE_TFA_VERIFY_TOTP:
                /*
                Using provided TFA user interface. A selection callback is available using "setSelectionCallback" method for
                additional flow control.
                 */
                TFATOTPVerificationFragment tfatotpVerificationFragment = TFATOTPVerificationFragment.newInstance();
                tfatotpVerificationFragment.setResolverFactory(mViewModel.getTFAResolverFactory());
                tfatotpVerificationFragment.show(getSupportFragmentManager(), "TFATOTPVerificationFragment");
                break;
        }
        dataRoute.setObserved(true);
    };

    /*
    Error observer.
     */
    private Observer<ErrorEvent> errorObserver = event -> {
        if (event.isObserved()) {
            return;
        }
        updateLoginProgress(false);
        Log.e("LoginActivity", event.getError().getLocalizedMessage());

        // Show error alert dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Error").setMessage(event.getError().getLocalizedMessage());
        builder.setPositiveButton(android.R.string.ok, (dialog, id) -> dialog.dismiss());
        builder.setNegativeButton(android.R.string.cancel, (dialog, id) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();

        event.setObserved(true);
    };

    /*
    Update login progress bar state according to... wait for it.......... login progress state.
     */
    private void updateLoginProgress(boolean visible) {
        if (mLoginProgress == null) {
            mLoginProgress = findViewById(R.id.login_progress);
        }
        mLoginProgress.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /**
     * Event click for Facebook social sign in button.
     */
    public void onFacebookLoginClicked(View clickView) {
        updateLoginProgress(true);
        mViewModel.loginWithSocialProvider(FACEBOOK);
    }

    /**
     * Event click for Google social sign in button.
     */
    public void onGoogleLoginClicked(View clickView) {
        updateLoginProgress(true);
        mViewModel.loginWithSocialProvider(GOOGLE);
    }

    /**
     * Event click for forgot password button.
     */
    public void onForgotPasswordClicked(View clickedView) {
        final ForgotPasswordBottomSheet frag = ForgotPasswordBottomSheet.newInstance();
        frag.show(getSupportFragmentManager(), ForgotPasswordBottomSheet.TAG);
    }

    /**
     * Event click for sign in button.
     */
    public void onSignInClicked(View clickView) {
        if (mScreenSetsCheckbox.isChecked()) {

            // Progress implemented on screenset.
            mViewModel.useScreenSetsForSignIn();
        } else {
            if (validateInput()) {

                updateLoginProgress(true);

                mViewModel.signInUsingUsernameAndPassword(
                        mUserName.getText().toString().trim(),
                        mPassword.getText().toString().trim()
                );
            }
        }
    }

    /**
     * Event click for sign up text.
     */
    public void onSignUpClicked(View clickView) {
        if (mScreenSetsCheckbox.isChecked()) {

            // Progress implemented on screenset.
            mViewModel.useScreenSetsFeatureForSignUp();
        } else {
            if (validateInput()) {

                updateLoginProgress(true);

                mViewModel.signUpUsingUsernameAndPassword(
                        mUserName.getText().toString().trim(),
                        mPassword.getText().toString().trim()
                );
            }
        }
    }

    /**
     * Click to open site change bottom sheet dialog fragment. In this option you will be able to change
     * your demo website to overview different & "complex" flows such as pending registration & TFA.
     */
    public void onSiteConfigurationClicked(View clickedView) {
        final DemoSiteChangeBottomSheet frag = DemoSiteChangeBottomSheet.newInstance();
        frag.show(getSupportFragmentManager(), DemoSiteChangeBottomSheet.TAG);
    }

    /*
     You get it right?
    */
    private boolean validateInput() {
        final String username = mUserName.getText().toString().trim();
        final String password = mPassword.getText().toString().trim();
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            centerToastWith("Missing input field... This application will explode in 3...2...1...");
            return false;
        }
        return true;
    }
}



