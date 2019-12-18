package com.gigya.android.gigyademo.ui.sheets;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.gigya.android.gigyademo.R;
import com.gigya.android.gigyademo.model.DataEvent;
import com.gigya.android.sdk.interruption.tfa.models.TFAProviderModel;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.tfa.GigyaDefinitions;

import java.util.List;

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
    private List<TFAProviderModel> mProviders;

    public static final String TAG = "TFAProviderSelectionBottomSheet";

    public static TFAProviderSelectionBottomSheet newInstance(int sourceError) {
        final Bundle args = new Bundle();
        args.putInt("sourceError", sourceError);
        final TFAProviderSelectionBottomSheet sheet = new TFAProviderSelectionBottomSheet();
        sheet.setArguments(args);
        return sheet;
    }

    public void setProviders(List<TFAProviderModel> list) {
        mProviders = list;
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
        setVisibleProviders(view);

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

    private void setVisibleProviders(View view) {
        final RadioButton totpB = view.findViewById(R.id.totp_tfa);
        final RadioButton phoneB = view.findViewById(R.id.phone_tfa);

        if (mSourceError == GigyaError.Codes.ERROR_PENDING_TWO_FACTOR_VERIFICATION) {
            TFAProviderModel model = mProviders.get(0);
            if (model.getName().equals(GigyaDefinitions.TFAProvider.TOTP)) {
                totpB.setVisibility(View.VISIBLE);
                totpB.setChecked(true);
            } else if (model.getName().equals(GigyaDefinitions.TFAProvider.PHONE)) {
                phoneB.setVisibility(View.VISIBLE);
                phoneB.setChecked(true);
            }
        } else {
            totpB.setVisibility(View.VISIBLE);
            phoneB.setVisibility(View.VISIBLE);
        }
    }
}
