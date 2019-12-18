package com.gigya.android.gigyademo.ui.activity;

import android.app.Activity;
import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;

import com.gigya.android.gigyademo.model.CustomAccount;
import com.gigya.android.gigyademo.model.DataEvent;
import com.gigya.android.gigyademo.model.ErrorEvent;
import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.GigyaPluginCallback;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.auth.GigyaAuth;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.tfa.GigyaTFA;

import java.util.HashMap;

import static com.gigya.android.gigyademo.model.DataEvent.ROUTE_GET_ACCOUNT_INFO;

public class AccountViewModel extends AndroidViewModel {

    final private Gigya<CustomAccount> mGigya;

    final private GigyaAuth mGigyaAuth = GigyaAuth.getInstance();
    final private GigyaTFA mGigyaTFA = GigyaTFA.getInstance();

    public AccountViewModel(@NonNull Application application) {
        super(application);
        mGigya = Gigya.getInstance(CustomAccount.class);
    }

    final MutableLiveData<DataEvent> mDataRouter = new MutableLiveData<>();
    final MutableLiveData<ErrorEvent> mErrorRouter = new MutableLiveData<>();

    boolean isLoggedIn() {
        return mGigya.isLoggedIn();
    }

    /**
     * Request updated account information.
     * Using invalidate cache in order to refresh account information every time it is requested.
     * If a refresh is not needed by demand you can use it without "invalidateCache" in order to utilize
     * the account caching mechanism.
     *
     * @see {https://developers.gigya.com/display/GD/Android+SDK+v4#AndroidSDKv4-AccountHandling}
     */
    void getUpdatedAccountInformation() {
        mGigya.getAccount(true, new GigyaLoginCallback<CustomAccount>() {
            @Override
            public void onSuccess(CustomAccount customAccount) {
                mDataRouter.postValue(new DataEvent(ROUTE_GET_ACCOUNT_INFO, customAccount));
            }

            @Override
            public void onError(GigyaError gigyaError) {
                mErrorRouter.postValue(new ErrorEvent(gigyaError));
            }
        });
    }

    /**
     * Log out of current live session.
     */
    void logoutOfGigyaAccount() {
        mGigya.logout();
    }

    /**
     * Show profile update screenset.
     */
    void showAccountInfoScreenSet() {
        mGigya.showScreenSet(
                "Default-ProfileUpdate",
                true,
                new HashMap<>(),
                new GigyaPluginCallback<CustomAccount>() {
                    @Override
                    public void onConnectionAdded() {


                    }
                }
        );
    }

    /**
     * Registering both TFA & authentication library.
     * <p>
     * todo link to auth wiki section.
     * <p>
     * todo link to TFA wiki section.
     *
     * @param activityInstance Current activity instance.
     */
    void registerForRemoteNotifications(Activity activityInstance) {
        mGigyaAuth.registerForPushNotifications(activityInstance);
        mGigyaTFA.registerForRemoteNotifications(activityInstance);
    }

    void registerForPushAuthentication() {
        mGigyaAuth.registerForAuthPush(new GigyaCallback<GigyaApiResponse>() {
            @Override
            public void onSuccess(GigyaApiResponse response) {
                mDataRouter.postValue(
                        new DataEvent(
                                DataEvent.ROUTE_AUTH_DEVICE_REGISTER,
                                response
                        )
                );
            }

            @Override
            public void onError(GigyaError gigyaError) {
                mErrorRouter.postValue(
                        new ErrorEvent(
                                gigyaError
                        )
                );
            }
        });
    }

    void optInForPushTFA() {
        GigyaTFA.getInstance().optInForPushTFA(new GigyaCallback<GigyaApiResponse>() {
            @Override
            public void onSuccess(GigyaApiResponse response) {
                mDataRouter.postValue(
                        new DataEvent(
                                DataEvent.ROUTE_OPT_IN_FOR_PUSH_TFA,
                                response
                        )
                );
            }

            @Override
            public void onError(GigyaError gigyaError) {
                mErrorRouter.postValue(
                        new ErrorEvent(
                                gigyaError
                        )
                );
            }

        });
    }
}
