package com.gigya.android.sdk.tfa.ui;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.gigya.android.sdk.tfa.R;

import java.util.ArrayList;

public class TFAProviderSelectionFragment extends BaseTFAFragment {

    public static final String ARG_PROVIDER_LIST = "arg_provider_list";

    public interface SelectionCallback {

        void onProviderSelected(@NonNull String selectedProvider);

        void onDismiss();
    }

    private SelectionCallback _selectionCallback;

    public void setSelectionCallback(SelectionCallback selectionCallback) {
        _selectionCallback = selectionCallback;
    }

    public static TFAProviderSelectionFragment newInstance(ArrayList<String> providers) {
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_PROVIDER_LIST, providers);
        TFAProviderSelectionFragment fragment = new TFAProviderSelectionFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getLayoutId() {
        return R.layout.fragment_provider_selection;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (getArguments() != null && getActivity() != null) {
            final ArrayList<String> providers = getArguments().getStringArrayList(ARG_PROVIDER_LIST);
            if (providers == null) {
                if (_selectionCallback != null) {
                    _selectionCallback.onDismiss();
                }
                dismiss();
                return;
            }

            final Spinner spinner = view.findViewById(R.id.fps_provider_spinner);
            final ArrayAdapter providerAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, providers);
            spinner.setAdapter(providerAdapter);

            final Button selectionButton = view.findViewById(R.id.fps_select_button);
            selectionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (_selectionCallback != null) {
                        final String selectedProvider = spinner.getSelectedItem().toString();
                        _selectionCallback.onProviderSelected(selectedProvider);
                        dismiss();
                    }
                }
            });

            final Button dismissButton = view.findViewById(R.id.fps_dismiss_button);
            dismissButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (_selectionCallback != null) {
                        _selectionCallback.onDismiss();
                        dismiss();
                    }
                }
            });
        } else {
            dismiss();
        }
    }


}
