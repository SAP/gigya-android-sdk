package com.gigya.android.sdk.tfa.push;

import android.support.annotation.NonNull;

public interface ITFANotifier {

    void notifyWith(@NonNull String title, @NonNull String body);
}
