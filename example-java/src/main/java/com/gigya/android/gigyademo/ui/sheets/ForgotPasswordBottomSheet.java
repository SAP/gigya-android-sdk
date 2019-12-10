package com.gigya.android.gigyademo.ui.sheets;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.gigya.android.gigyademo.R;

public class ForgotPasswordBottomSheet extends AbstractLoginBottomSheet {

    public static final String TAG = "ForgotPasswordBottomSheet";

    public static ForgotPasswordBottomSheet newInstance() {
        return new ForgotPasswordBottomSheet();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_forgot_password_sheet;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // Reference views.
        final TextInputLayout emailInputLayout = view.findViewById(R.id.email_input_layout);
        final Button submitButton = view.findViewById(R.id.submit_button);

        submitButton.setOnClickListener(view1 -> {
            final String email = emailInputLayout.getEditText().getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(getContext(), "Sending email into the void will probably wont work...", Toast.LENGTH_SHORT).show();
                return;
            }
            mViewModel.forgotPassword(email);
            dismiss();
        });
    }



}
