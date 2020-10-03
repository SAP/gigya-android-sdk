package com.gigya.android.gigyademo.ui.activity;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.gigya.android.gigyademo.R;
import com.gigya.android.gigyademo.model.CustomAccount;
import com.gigya.android.gigyademo.model.DataEvent;
import com.gigya.android.gigyademo.model.ErrorEvent;
import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.GigyaPluginCallback;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.interruption.IPendingRegistrationResolver;
import com.gigya.android.sdk.interruption.tfa.TFAResolverFactory;
import com.gigya.android.sdk.interruption.tfa.models.TFAProviderModel;
import com.gigya.android.sdk.network.GigyaError;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.Context.MODE_PRIVATE;

public class LoginViewModel extends AndroidViewModel {

    private Gigya<CustomAccount> mGigya;

    final MutableLiveData<DataEvent> mDataRouter = new MutableLiveData<>();
    final MutableLiveData<ErrorEvent> mErrorRouter = new MutableLiveData<>();

    @Nullable
    private TFAResolverFactory mResolverFactory;

    @Nullable
    private IPendingRegistrationResolver mPendingRegistrationResolver;

    @Nullable
    public TFAResolverFactory getTFAResolverFactory() {
        return mResolverFactory;
    }

    public LoginViewModel(@NonNull Application application) {
        super(application);
        mGigya = Gigya.getInstance(CustomAccount.class);
    }

    public MutableLiveData<DataEvent> getDataRouter() {
        return mDataRouter;
    }

    @Override
    protected void onCleared() {
        // Clear resolver factory reference.
        mResolverFactory = null;
        super.onCleared();
    }

    /**
     * Change the demo site configuration.
     * 1 - Default.
     * 2 - Force pending registration.
     * 3 - RBA Level 30.
     *
     * @param selectedViewId Selection view (RadionButton id).
     */
    public void changeSiteConfiguration(int selectedViewId) {
        String apiKey;
        switch (selectedViewId) {
            case R.id.pending_registration_button:
                apiKey = getApplication().getString(R.string.force_pending_registration_api_key);
                break;
            case R.id.rba_level_20_button:
                apiKey = getApplication().getString(R.string.rba_20_api_key);
                break;
            default:
                apiKey = getApplication().getString(R.string.default_api_key);
                break;
        }
        Gigya.getInstance(CustomAccount.class).init(apiKey);
        /*
        Updating key in shared preferences to allow the app to restart with the same key.
         */
        getApplication().getSharedPreferences("demo", MODE_PRIVATE).edit().putString("savedKey", apiKey).apply();
    }


    /**
     * Begin simple login procedure using username & password.
     *
     * @param username Selected username.
     * @param password Selected password.
     */
    void signInUsingUsernameAndPassword(String username, String password) {
        mGigya.login(
                username,
                password,
                new GigyaLoginCallback<CustomAccount>() {

                    /*
                    Note:
                    When overriding login callback methods make sure you remove the "super" call.
                     */

                    @Override
                    public void onSuccess(CustomAccount customAccount) {
                        mDataRouter.postValue(
                                new DataEvent(
                                        DataEvent.ROUTE_LOGIN_SUCCESS,
                                        customAccount
                                )
                        );
                    }

                    @Override
                    public void onError(GigyaError gigyaError) {
                        /*
                        Error may be recoverable recoverable. Override additional callbacks if needed.
                         */
                        mErrorRouter.postValue(
                                new ErrorEvent(
                                        gigyaError
                                )
                        );
                    }

                    @Override
                    public void onOperationCanceled() {
                        mDataRouter.postValue(
                                new DataEvent(
                                        DataEvent.ROUTE_OPERATION_CANCELED,
                                        null
                                )
                        );
                    }

                    @Override
                    public void onPendingRegistration(@NonNull GigyaApiResponse response,
                                                      @NonNull IPendingRegistrationResolver resolver) {
                        mPendingRegistrationResolver = resolver;
                        mDataRouter.postValue(
                                new DataEvent(
                                        DataEvent.ROUTE_PENDING_REGISTRATION,
                                        response
                                )
                        );
                    }

                    @Override
                    public void onPendingTwoFactorRegistration(@NonNull GigyaApiResponse response,
                                                               @NonNull List<TFAProviderModel> inactiveProviders,
                                                               @NonNull TFAResolverFactory resolverFactory) {
                        mResolverFactory = resolverFactory;
                        mDataRouter.postValue(
                                new DataEvent(
                                        DataEvent.ROUTE_TFA_PROVIDER_SELECTION,
                                        new Pair<>(GigyaError.Codes.ERROR_PENDING_TWO_FACTOR_REGISTRATION,
                                                inactiveProviders)

                                )
                        );
                    }

                    @Override
                    public void onPendingTwoFactorVerification(@NonNull GigyaApiResponse response,
                                                               @NonNull List<TFAProviderModel> activeProviders,
                                                               @NonNull TFAResolverFactory resolverFactory) {
                        mResolverFactory = resolverFactory;
                        mDataRouter.postValue(
                                new DataEvent(
                                        DataEvent.ROUTE_TFA_PROVIDER_SELECTION,
                                        new Pair<>(GigyaError.Codes.ERROR_PENDING_TWO_FACTOR_VERIFICATION,
                                                activeProviders)
                                )
                        );
                    }
                });
    }


    /**
     * Begin simple registration procedure using username & password.
     * In this example the session expiration is set to 0.
     * If you wish to provide a session expiration in code, add it to the register interface "parameters" map.
     *
     * @param username Selected username.
     * @param password Selected password.
     */
    void signUpUsingUsernameAndPassword(String username, String password) {
        /*
        Empty parameter map. Feel free to mutate.
         */
        final Map<String, Object> parameters = new HashMap<>();
        mGigya.register(
                username,
                password,
                parameters,
                new GigyaLoginCallback<CustomAccount>() {

                     /*
                    Note:
                    When overriding login callback methods make sure you remove the "super" call.
                     */

                    @Override
                    public void onSuccess(CustomAccount customAccount) {
                        mDataRouter.postValue(
                                new DataEvent(
                                        DataEvent.ROUTE_LOGIN_SUCCESS,
                                        customAccount
                                )
                        );
                    }

                    @Override
                    public void onError(GigyaError gigyaError) {
                        /*
                        Error may be recoverable recoverable. Override additional callbacks if needed.
                        */
                        mErrorRouter.postValue(
                                new ErrorEvent(
                                        gigyaError
                                )
                        );
                    }

                    @Override
                    public void onOperationCanceled() {
                        mDataRouter.postValue(
                                new DataEvent(
                                        DataEvent.ROUTE_OPERATION_CANCELED,
                                        null
                                )
                        );
                    }

                    @Override
                    public void onPendingRegistration(@NonNull GigyaApiResponse response,
                                                      @NonNull IPendingRegistrationResolver resolver) {
                        mPendingRegistrationResolver = resolver;
                        mDataRouter.postValue(
                                new DataEvent(
                                        DataEvent.ROUTE_PENDING_REGISTRATION,
                                        response
                                )
                        );
                    }

                    @Override
                    public void onPendingTwoFactorRegistration(@NonNull GigyaApiResponse response,
                                                               @NonNull List<TFAProviderModel> inactiveProviders,
                                                               @NonNull TFAResolverFactory resolverFactory) {
                        mResolverFactory = resolverFactory;
                        mDataRouter.postValue(
                                new DataEvent(
                                        DataEvent.ROUTE_TFA_PROVIDER_SELECTION,
                                        new Pair<>(GigyaError.Codes.ERROR_PENDING_TWO_FACTOR_REGISTRATION,
                                                inactiveProviders)
                                )
                        );
                    }

                    @Override
                    public void onPendingTwoFactorVerification(@NonNull GigyaApiResponse response,
                                                               @NonNull List<TFAProviderModel> activeProviders,
                                                               @NonNull TFAResolverFactory resolverFactory) {
                        mResolverFactory = resolverFactory;
                        mDataRouter.postValue(
                                new DataEvent(
                                        DataEvent.ROUTE_TFA_PROVIDER_SELECTION,
                                        new Pair<>(GigyaError.Codes.ERROR_ACCOUNT_PENDING_VERIFICATION,
                                                activeProviders)
                                )
                        );
                    }

                });
    }

    /**
     * Begin selected social provider login flow.
     *
     * @param provider Selected social provider.
     */
    void loginWithSocialProvider(@GigyaDefinitions.Providers.SocialProvider String provider) {
        /*
        Use any additional parameters if needed.
         */
        final Map<String, Object> additionalParameters = new HashMap<>();
        mGigya.login(
                provider,
                additionalParameters,
                new GigyaLoginCallback<CustomAccount>() {
                    @Override
                    public void onSuccess(CustomAccount customAccount) {
                        mDataRouter.postValue(new DataEvent(
                                        DataEvent.ROUTE_LOGIN_SUCCESS,
                                        customAccount
                                )
                        );
                    }

                    @Override
                    public void onError(GigyaError gigyaError) {
                        /*
                        Error may be recoverable recoverable. Override additional callbacks if needed.
                         */
                        mErrorRouter.postValue(
                                new ErrorEvent(
                                        gigyaError
                                )
                        );
                    }
                });
    }

    /**
     * Will open default RegistrationLogin screenset.
     */
    void useScreenSetsForSignIn() {
         /*
        Use any additional parameters if needed.
         */
        final Map<String, Object> additionalParameters = new HashMap<>();
        mGigya.showScreenSet(
                "Default-RegistrationLogin",
                true,
                additionalParameters,
                new GigyaPluginCallback<CustomAccount>() {

                    @Override
                    public void onCanceled() {
                        mDataRouter.postValue(
                                new DataEvent(
                                        DataEvent.ROUTE_OPERATION_CANCELED,
                                        null
                                )
                        );
                    }

                    @Override
                    public void onLogin(@NonNull CustomAccount customAccount) {
                        mDataRouter.postValue(
                                new DataEvent(
                                        DataEvent.ROUTE_LOGIN_SUCCESS,
                                        customAccount
                                )
                        );
                    }
                }
        );
    }

    /**
     * Will open default RegistrationLogin screenset in registration start screen.
     */
    void useScreenSetsFeatureForSignUp() {
        /*
        Use any additional parameters if needed.
         */
        final Map<String, Object> additionalParameters = new HashMap<>();
        additionalParameters.put("startScreen", "gigya-register-screen");
        mGigya.showScreenSet(
                "Default-RegistrationLogin",
                true,
                additionalParameters,
                new GigyaPluginCallback<CustomAccount>() {

                    @Override
                    public void onLogin(@NonNull CustomAccount customAccount) {
                        mDataRouter.postValue(
                                new DataEvent(
                                        DataEvent.ROUTE_LOGIN_SUCCESS,
                                        customAccount
                                )
                        );
                    }
                }
        );
    }

    /**
     * Send an password reset email to given login id.
     *
     * @param loginId Login id / email address.
     */
    public void forgotPassword(String loginId) {
        mGigya.forgotPassword(
                loginId,
                new GigyaLoginCallback<GigyaApiResponse>() {
                    @Override
                    public void onSuccess(GigyaApiResponse response) {
                        mDataRouter.postValue(
                                new DataEvent(
                                        DataEvent.ROUTE_FORGOT_PASSWORD_EMAIL_SENT,
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

    /**
     * Attempt to resolve a pending registration interruption via updating the account
     * information with the missing mandatory profile field.
     * <p>
     * Note:
     * This flow is relevant only for the provided test site.
     *
     * @param zip String zip code value.
     */
    public void resolvePendingRegistration(String zip) {
        if (mPendingRegistrationResolver != null) {
            final Map<String, Object> parameterMap = new HashMap<>();
            parameterMap.put("profile", "{ \"zip\": " + zip + " }");
            mPendingRegistrationResolver.setAccount(parameterMap);
        }
    }
}
