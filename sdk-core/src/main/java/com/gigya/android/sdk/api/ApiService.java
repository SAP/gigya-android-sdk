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
import com.gigya.android.sdk.persistence.IPersistenceService;
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
    final private IPersistenceService _psService;

    public ApiService(Config config,
                      IRestAdapter adapter,
                      IApiRequestFactory reqFactory,
                      IPersistenceService psService) {
        _config = config;
        _adapter = adapter;
        _reqFactory = reqFactory;
        _psService = psService;
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
    public void send(GigyaApiRequest request, IApiServiceResponse apiCallback) {
        send(request, false, apiCallback);
    }

    @Override
    public void send(final GigyaApiRequest request, boolean blocking, final IApiServiceResponse apiCallback) {
        GigyaLogger.debug(LOG_TAG, "sending: " + request.getApi());
        GigyaLogger.debug(LOG_TAG, "sending: params = " + request.getParams().toString());

        _adapter.send(request, blocking, new IRestAdapterCallback() {
            @Override
            public void onResponse(String jsonResponse, String responseDateHeader) {

                updateOffset(responseDateHeader);

                final GigyaApiResponse apiResponse = new GigyaApiResponse(jsonResponse);
                final int apiErrorCode = apiResponse.getErrorCode();

                GigyaLogger.debug(LOG_TAG, "SEND REQUEST with:\n" + jsonResponse);

                // Check for timestamp skew error.
                if (isRequestExpiredError(apiErrorCode)) {

                    GigyaLogger.error(LOG_TAG, "Request expired error occurred. Allowing retries");

                    new RetryDispatcher.Builder(_adapter, _reqFactory)
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
                } else if (isInvalidGMIDError(apiResponse)) {
                    handleInvalidGMIDError(apiResponse, request, apiCallback);
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

    private boolean refreshGmid() {
        if (_config.getGmid() == null) {
            return true;
        }
        long gmidRefreshTime = _config.getGmidRefreshTime() == 0 ?
                _psService.getGmidRefreshTime() : _config.getGmidRefreshTime();
        if (gmidRefreshTime == 0) {
            return true;
        }
        long currentTimeInMillis = System.currentTimeMillis();
        return gmidRefreshTime < currentTimeInMillis;
    }

    @Override
    public void getSdkConfig(final IApiServiceResponse apiCallback) {
        // Loading updated GMID/UCID to config.
        loadIds();

        // Checking if we should request the GMID.
        if (!refreshGmid()) {
            GigyaLogger.debug(LOG_TAG, "GMID refresh time not passed");
            return;
        }
        GigyaLogger.debug(LOG_TAG, "GMID refresh time passed - requesting ids");
        GigyaLogger.debug(LOG_TAG, "sending: " + GigyaDefinitions.API.API_GET_IDS);

        final Map<String, Object> params = new HashMap<>();
        //params.put("include", "permissions,ids,appIds");
        final GigyaApiRequest request = _reqFactory.create(
                GigyaDefinitions.API.API_GET_IDS,
                params,
                RestAdapter.HttpMethod.POST);
        // Set request as anonymous! Will not go through if will include timestamp, nonce & signature.
        request.setAnonymous(true);
        send(request, true, new IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse apiResponse) {
                final int apiErrorCode = apiResponse.getErrorCode();
                if (apiErrorCode == 0) {

                    final String gmid = apiResponse.getField("gcid", String.class);
                    final String ucid = apiResponse.getField("ucid", String.class);

                    // Get gmidRefresh time.
                    final Long refreshTime = apiResponse.getField("refreshTime", Long.class);

                    if (gmid == null || ucid == null) {
                        // Parsing error.
                        apiCallback.onApiError(GigyaError.fromResponse(apiResponse));
                        onConfigError();
                        return;
                    }
                    onConfigResponse(gmid, ucid, refreshTime == null ? 0 : refreshTime);
                    apiCallback.onApiSuccess(apiResponse);
                } else {
                    apiCallback.onApiError(GigyaError.fromResponse(apiResponse));
                    onConfigError();
                }
            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                apiCallback.onApiError(gigyaError);
                onConfigError();
            }
        });
    }

    private void loadIds() {
        final String gmid = _psService.getGmid();
        if (gmid != null) {
            _config.setGmid(gmid);
        }
        final String ucid = _psService.getUcid();
        if (ucid != null) {
            _config.setUcid(ucid);
        }
    }

    private void onConfigResponse(String gmid, String ucid, long refreshTime) {
        _config.setGmid(gmid);
        _config.setUcid(ucid);
        _config.setGmidRefreshTime(refreshTime);

        // Update prefs.
        _psService.setGmid(gmid);
        _psService.setUcid(ucid);
        _psService.setGmidRefreshTime(refreshTime);

        release();
    }

    private void onConfigError() {
        release();
    }

    //endregion

    private boolean isRequestExpiredError(int code) {
        return code == GigyaError.Codes.ERROR_REQUEST_HAS_EXPIRED;
    }

    //region Invalid GMID error handling

    private boolean isInvalidGMIDError(GigyaApiResponse response) {
        return new InvalidGMIDResponseEvaluator().evaluate(response);
    }

    private void handleInvalidGMIDError(GigyaApiResponse response,
                                        final GigyaApiRequest originalRequest,
                                        final IApiServiceResponse apiCallback) {
        // Create new ids request.
        final Map<String, Object> params = new HashMap<>();
        //params.put("include", "permissions,ids,appIds");
        final GigyaApiRequest request = _reqFactory.create(
                GigyaDefinitions.API.API_GET_IDS,
                params,
                RestAdapter.HttpMethod.POST);
        // Set request as anonymous! Will not go through if will include timestamp, nonce & signature.
        request.setAnonymous(true);
        send(request, true, new IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse apiResponse) {
                final int apiErrorCode = apiResponse.getErrorCode();
                if (apiErrorCode == 0) {

                    final String gmid = apiResponse.getField("gcid", String.class);
                    final String ucid = apiResponse.getField("ucid", String.class);

                    // Get gmidRefresh time.
                    final Long refreshTime = apiResponse.getField("refreshTime", Long.class);
                    if (gmid == null || ucid == null) {
                        onConfigError();
                        return;
                    }
                    onConfigResponse(gmid, ucid, refreshTime == null ? 0 : refreshTime);

                    // Retry original request.
                    originalRequest.getParams().put("gmid", gmid);
                    originalRequest.getParams().put("ucid", ucid);

                    send(originalRequest, true, apiCallback);
                } else {
                    onConfigError();
                    apiCallback.onApiSuccess(apiResponse);
                }
            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                apiCallback.onApiError(gigyaError);
                onConfigError();
            }
        });
    }

    //endregion

}
