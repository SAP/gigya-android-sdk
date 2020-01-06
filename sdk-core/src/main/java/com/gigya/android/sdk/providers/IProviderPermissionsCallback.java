package com.gigya.android.sdk.providers;

import java.util.List;

public interface IProviderPermissionsCallback {

    void granted();

    void noAccess();

    void cancelled();

    void declined(List<String> declined);

    void failed(String error);
}
