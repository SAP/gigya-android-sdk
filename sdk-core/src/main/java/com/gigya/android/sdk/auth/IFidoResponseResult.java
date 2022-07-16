package com.gigya.android.sdk.auth;

import android.content.Intent;

interface IFidoResponseResult {

    void onIntent(int resultCode, Intent intent);
}
