package com.gigya.android.sdk.api;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.api.models.GigyaConfigModel;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.adapter.IRestAdapter;
import com.gigya.android.sdk.network.adapter.IRestAdapterCallback;
import com.gigya.android.sdk.network.adapter.RestAdapter;
import com.gigya.android.sdk.reporting.ReportingManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Service responsible for mediating and executing HTTP based api requests.
 */
public class ApiService implements IApiService {

    private static final String LOG_TAG = "ApiService";

    final private Config _config;
    final private IRestAdapter _adapter;
    final private IApiRequestFactory _reqFactory;

    public ApiService(Config config,
                      IRestAdapter adapter,
                      IApiRequestFactory reqFactory) {
        _config = config;
        _adapter = adapter;
        _reqFactory = reqFactory;
    }

    /*
    Main service comm interface.
     */
    public interface IApiServiceResponse {

        void onApiSuccess(GigyaApiResponse response);

        void onApiError(GigyaError gigyaError);
    }

    private static final String SERVER_TIMESTAMP_PATTERN = "EEE, dd MMM yyyy HH:mm:ss zzz";

    /**
     * Update main SDK interface with the current server offset value.
     *
     * @param dateHeader String date header field returned from last request.
     */
    private void updateOffset(String dateHeader) {
        if (dateHeader != null) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(SERVER_TIMESTAMP_PATTERN, Locale.ENGLISH);
                Date serverDate = format.parse(dateHeader);
                Long offset = (serverDate.getTime() - System.currentTimeMillis()) / 1000;

                GigyaLogger.debug(LOG_TAG, "updateOffset: Server timestamp = " + offset);

                _config.setServerOffset(offset);
            } catch (Exception ex) {
                GigyaLogger.error(LOG_TAG, "updateOffset: unable to update offset with exception");
                ReportingManager.get().error(Gigya.VERSION, "core", "ApiService: unable to update offset with exception");
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void send(final GigyaApiRequest request, boolean blocking, final IApiServiceResponse apiCallback) {
        if (!request.getApi().equals(GigyaDefinitions.API.API_GET_SDK_CONFIG) && requiresSdkConfig()) {
            // Need to verify if GMID is available. If not we must request SDK configuration.
            getSdkConfig(apiCallback, request.getTag());
        }

        GigyaLogger.debug(LOG_TAG, "sending: " + request.getApi());
        GigyaLogger.debug(LOG_TAG, "sending: params = " + request.getParams().toString());

        _adapter.send(request, blocking, new IRestAdapterCallback() {
            @Override
            public void onResponse(String jsonResponse, String responseDateHeader) {

                updateOffset(responseDateHeader);

                final GigyaApiResponse apiResponse = new GigyaApiResponse(jsonResponse);
                final int apiErrorCode = apiResponse.getErrorCode();

                // Check for timestamp skew error.
                if (isRequestExpiredError(apiErrorCode)) {

                    GigyaLogger.error(LOG_TAG, "Request expired error occurred. Allowing retries");

                    new RetryDispatcher.Builder(_adapter)
                            .request(request)
                            .errorCode(GigyaError.Codes.ERROR_REQUEST_HAS_EXPIRED)
                            .tries(2)
                            .handler(new RetryDispatcher.IRetryHandler() {
                                @Override
                                public void onCompleteWithResponse(GigyaApiResponse retryResponse) {
                                    apiCallback.onApiSuccess(retryResponse);
                                }

                                @Override
                                public void onCompleteWithError(GigyaError error) {
                                    apiCallback.onApiError(error);
                                }

                                @Override
                                public void onUpdateDate(String date) {
                                    updateOffset(date);
                                }
                            })
                            .dispatch();
                    return;
                }

                apiCallback.onApiSuccess(apiResponse);
            }

            @Override
            public void onError(GigyaError gigyaError) {
                apiCallback.onApiError(gigyaError);
            }

        });
    }

    @Override
    public void release() {
        _adapter.release();
    }

    @Override
    public void cancel(String tag) {
        _adapter.cancel(tag);
    }


    //region SDK CONFIG

    private boolean requiresSdkConfig() {
        return (_config.getGmid() == null || _config.getServerOffset() == null);
    }

    private void onConfigResponse(GigyaConfigModel response) {
        _config.setUcid(response.getIds().getUcid());
        _config.setGmid(response.getIds().getGmid());
        release();
    }

    private void onConfigError(String nextApiTag) {
        if (nextApiTag != null) {
            cancel(nextApiTag);
        }
        release();
    }

    private void getSdkConfig(final IApiServiceResponse apiCallback, final String nextApiTag) {

        GigyaLogger.debug(LOG_TAG, "sending: " + GigyaDefinitions.API.API_GET_SDK_CONFIG);

        final Map<String, Object> params = new HashMap<>();
        params.put("include", "permissions,ids,appIds");
        final GigyaApiRequest request = _reqFactory.create(
                GigyaDefinitions.API.API_GET_SDK_CONFIG,
                params,
                RestAdapter.HttpMethod.POST);
        // Set request as anonymous! Will not go through if will include timestamp, nonce & signature.
        request.setAnonymous(true);
        send(request, true, new IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse apiResponse) {
                final int apiErrorCode = apiResponse.getErrorCode();
                if (apiErrorCode == 0) {
                    final GigyaConfigModel parsed = apiResponse.parseTo(GigyaConfigModel.class);
                    if (parsed == null) {
                        // Parsing error.
                        apiCallback.onApiError(GigyaError.fromResponse(apiResponse));
                        onConfigError(nextApiTag);
                        return;
                    }
                    onConfigResponse(parsed);
                } else {
                    apiCallback.onApiError(GigyaError.fromResponse(apiResponse));
                    onConfigError(nextApiTag);
                }
            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                apiCallback.onApiError(gigyaError);
                onConfigError(nextApiTag);
            }
        });
    }

    //endregion

    private boolean isRequestExpiredError(int code) {
        return code == GigyaError.Codes.ERROR_REQUEST_HAS_EXPIRED;
    }

}
