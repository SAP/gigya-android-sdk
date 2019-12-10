package com.gigya.android.gigyademo.ui.sheets;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;

import com.gigya.android.gigyademo.R;
import com.gigya.android.gigyademo.model.DataEvent;
import com.gigya.android.sdk.network.GigyaError;

/**
 * Bottom sheet dialog used for available TFA providers.
 * The demo application is using configuration for phone/totp provider setup only. Therefore this
 * is hardcoded within the sheet ui.
 * For a more dynamic setup feel free to look at:
 *
 * @see com.gigya.android.sdk.tfa.ui.TFAProviderSelectionFragment class.
 */
public class TFAProviderSelectionBottomSheet extends AbstractLoginBottomSheet {

    private int mSourceError;

    public static final String TAG = "TFAProviderSelectionBottomSheet";

    public static TFAProviderSelectionBottomSheet newInstance(int sourceError) {
        final Bundle args = new Bundle();
        args.putInt("sourceError", sourceError);
        final TFAProviderSelectionBottomSheet sheet = new TFAProviderSelectionBottomSheet();
        sheet.setArguments(args);
        return sheet;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_tfa_provider_sheet;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);

        if (getArguments() != null) {
            mSourceError = getArguments().getInt("sourceError");
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        // Close button.
        view.findViewById(R.id.close_button).setOnClickListener(v -> {
            mViewModel.getDataRouter().postValue(new DataEvent(DataEvent.ROUTE_OPERATION_CANCELED, null));
            dismiss();
        });

        final RadioGroup rg = view.findViewById(R.id.selection_group);

        final Button submitButton = view.findViewById(R.id.submit_button);
        submitButton.setOnClickListener(buttonView -> {
            if (mSourceError == 0) {
                return;
            }
            final int id = rg.getCheckedRadioButtonId();
            if (id == R.id.phone_tfa) {
                mViewModel.getDataRouter().postValue(
                        mSourceError == GigyaError.Codes.ERROR_PENDING_TWO_FACTOR_REGISTRATION
                                ?
                                new DataEvent(
                                        DataEvent.ROUTE_TFA_REGISTER_PHONE,
                                        null)
                                :
                                new DataEvent(
                                        DataEvent.ROUTE_TFA_VERIFY_PHONE,
                                        null)
                );
            } else if (id == R.id.totp_tfa) {
                mViewModel.getDataRouter().postValue(
                        mSourceError == GigyaError.Codes.ERROR_PENDING_TWO_FACTOR_REGISTRATION
                                ?
                                new DataEvent(
                                        DataEvent.ROUTE_TFA_REGISTER_TOTP,
                                        null)
                                :
                                new DataEvent(
                                        DataEvent.ROUTE_TFA_VERIFY_TOTP,
                                        null)
                );
            }
            dismiss();
        });
    }
}
