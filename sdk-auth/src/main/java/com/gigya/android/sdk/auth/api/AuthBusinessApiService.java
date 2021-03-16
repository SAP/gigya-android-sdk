package com.gigya.android.sdk.auth.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.account.IAccountService;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.api.BusinessApiService;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.api.IApiRequestFactory;
import com.gigya.android.sdk.api.IApiService;
import com.gigya.android.sdk.auth.GigyaOTPCallback;
import com.gigya.android.sdk.auth.GigyaDefinitions;
import com.gigya.android.sdk.auth.resolvers.OTPRegistrationResolver;
import com.gigya.android.sdk.interruption.IInterruptionResolverFactory;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.adapter.RestAdapter;
import com.gigya.android.sdk.providers.IProviderFactory;
import com.gigya.android.sdk.session.ISessionService;

import java.util.HashMap;
import java.util.Map;

/**
 * Authentication extension for business api service.
 * Contains authentication specific flow interfaces.
 */
public class AuthBusinessApiService extends BusinessApiService implements IAuthBusinessApiService {

    private static final String LOG_TAG = "AuthBusinessApiService";

    @SuppressWarnings("unchecked")
    public AuthBusinessApiService(ISessionService sessionService,
                                  IAccountService accountService,
                                  IApiService apiService,
                                  IApiRequestFactory requestFactory,
                                  IProviderFactory providerFactory,
                                  IInterruptionResolverFactory interruptionsHandler) {
        super(sessionService, accountService, apiService, requestFactory, providerFactory, interruptionsHandler);
    }

    //region PUSH

    /**
     * Register device for TFA push notification services.
     *
     * @param deviceInfo    Device information string representation.
     * @param gigyaCallback Request completion callback/handler.
     */
    @Override
    public void registerDevice(@NonNull final String deviceInfo, @NonNull final GigyaCallback<GigyaApiResponse> gigyaCallback) {
        if (!_sessionService.isValid()) {
            GigyaLogger.error(LOG_TAG, "registerDevice: session is invalid");
            gigyaCallback.onError(GigyaError.unauthorizedUser());
            return;
        }

        GigyaLogger.debug(LOG_TAG, "registerDevice: with device Info " + deviceInfo);

        final Map<String, Object> params = new HashMap<>();
        params.put("deviceInfo", deviceInfo);
        send(GigyaDefinitions.API.API_AUTH_DEVICE_REGISTER, params, RestAdapter.POST,
                GigyaApiResponse.class, new GigyaCallback<GigyaApiResponse>() {

                    @Override
                    public void onSuccess(GigyaApiResponse model) {
                        GigyaLogger.debug(LOG_TAG, "registerDevice: successfully registered device information");
                        gigyaCallback.onSuccess(model);
                    }

                    @Override
                    public void onError(GigyaError error) {
                        GigyaLogger.error(LOG_TAG, "registerDevice: failed to register device information");

                        gigyaCallback.onError(error);
                    }
                });
    }

    /**
     * Unregister device from backend push notification services.
     * <p>
     * NOTE: Interface/API is currently unavailable.
     *
     * @param gigyaCallback Request completion callback/handler.
     */
    @Override
    public void unregisterDevice(@NonNull GigyaCallback<GigyaApiResponse> gigyaCallback) {
        if (!_sessionService.isValid()) {
            GigyaLogger.error(LOG_TAG, "unregisterDevice: session is invalid");
            gigyaCallback.onError(GigyaError.unauthorizedUser());
            return;
        }

        GigyaLogger.error(LOG_TAG, "unregisterDevice: Feature currently unavailable");
        //((IAuthPersistenceService) _persistenceService).updateAuthPushState(false);
    }

    /**
     * Verify push notification token for flow completion.
     *
     * @param vToken        Verification token supplied in push notification data.
     * @param gigyaCallback Request completion callback/handler.
     */
    @Override
    public void verifyPush(@NonNull String vToken, @NonNull final GigyaCallback<GigyaApiResponse> gigyaCallback) {
        if (!_sessionService.isValid()) {
            GigyaLogger.error(LOG_TAG, "verifyPush: session is invalid");
            gigyaCallback.onError(GigyaError.unauthorizedUser());
            return;
        }

        GigyaLogger.debug(LOG_TAG, "verifyPush: with vToken " + vToken);

        final Map<String, Object> params = new HashMap<>();
        params.put("vToken", vToken);
        send(GigyaDefinitions.API.API_AUTH_PUSH_VERIFY, params, RestAdapter.POST,
                GigyaApiResponse.class, new GigyaCallback<GigyaApiResponse>() {

                    @Override
                    public void onSuccess(GigyaApiResponse model) {
                        GigyaLogger.debug(LOG_TAG, "verifyPush: successfully verified push authentication request");
                        gigyaCallback.onSuccess(model);
                    }

                    @Override
                    public void onError(GigyaError error) {
                        GigyaLogger.error(LOG_TAG, "verifyPush: failed to verify push authentication request with error " + error.getErrorCode());
                        gigyaCallback.onError(error);
                    }
                });
    }

    //endregion

    //region OTP

    /**
     * This method is used to trigger a Phone Number Login flow, or is part of an email code verification flow.
     * It accepts the user's phone number or email, returns a vToken, and sends an authentication code to the user.
     *
     * @param phoneNumber   User's phone number.
     * @param params        Parameter map.
     * @param gigyaCallback Request callback/handler specific for OTP flow.
     */
    @Override
    public <A extends GigyaAccount> void otpPhoneLogin(@Nullable String phoneNumber, @NonNull final Map<String, Object> params, @NonNull final GigyaOTPCallback<A> gigyaCallback) {
        if (phoneNumber == null) {
            GigyaLogger.error(LOG_TAG, "Trying to send otp code with no source");
            return;
        }
        params.put("phoneNumber", phoneNumber);
        if (!params.containsKey("lang")) {
            params.put("lang", "en");
        }
        send(GigyaDefinitions.API.API_AUTH_OTP_SEND_CODE, params, RestAdapter.POST,
                GigyaApiResponse.class, new GigyaCallback<GigyaApiResponse>() {

                    @Override
                    public void onSuccess(GigyaApiResponse model) {
                        GigyaLogger.debug(LOG_TAG, "otpSendCode: success");

                        // Generate resolver.
                        OTPRegistrationResolver<A> resolver = new OTPRegistrationResolver<A>(
                                gigyaCallback,
                                model,
                                AuthBusinessApiService.this,
                                params,
                                false
                        );

                        gigyaCallback.onPendingOTPVerification(model, resolver);
                    }

                    @Override
                    public void onError(GigyaError error) {
                        GigyaLogger.error(LOG_TAG, "otpSendCode: " + error.getErrorCode());
                        gigyaCallback.onError(error);
                    }
                });
    }


    /**
     * This method is used to trigger a Phone Number Update flow, or is part of an email code verification flow.
     * It accepts the user's phone number or email, returns a vToken, and sends an authentication code to the user.
     *
     * @param phoneNumber   User's phone number.
     * @param params        Parameter map.
     * @param gigyaCallback Request callback/handler specific for OTP flow.
     */
    @Override
    public <A extends GigyaAccount> void otpPhoneUpdate(@Nullable String phoneNumber, @NonNull final Map<String, Object> params, @NonNull final GigyaOTPCallback<A> gigyaCallback) {
        if (phoneNumber == null) {
            GigyaLogger.error(LOG_TAG, "Trying to send otp code with no source");
            return;
        }
        params.put("phoneNumber", phoneNumber);
        if (!params.containsKey("lang")) {
            params.put("lang", "en");
        }
        send(GigyaDefinitions.API.API_AUTH_OTP_SEND_CODE, params, RestAdapter.POST,
                GigyaApiResponse.class, new GigyaCallback<GigyaApiResponse>() {

                    @Override
                    public void onSuccess(GigyaApiResponse model) {
                        GigyaLogger.debug(LOG_TAG, "otpSendCode: success");

                        // Generate resolver.
                        OTPRegistrationResolver<A> resolver = new OTPRegistrationResolver<A>(
                                gigyaCallback,
                                model,
                                AuthBusinessApiService.this,
                                params,
                                true
                        );

                        gigyaCallback.onPendingOTPVerification(model, resolver);
                    }

                    @Override
                    public void onError(GigyaError error) {
                        GigyaLogger.error(LOG_TAG, "otpSendCode:  " + error.getErrorCode());
                        gigyaCallback.onError(error);
                    }
                });
    }

    private <A extends GigyaAccount> OTPRegistrationResolver<A> generateOTPResolver(
            GigyaApiResponse model,
            boolean update,
            Map<String, Object> params,
            GigyaOTPCallback<A> gigyaCallback) {
        return new OTPRegistrationResolver<A>(
                gigyaCallback,
                model,
                AuthBusinessApiService.this,
                params,
                true
        );
    }

    //endregion

}
