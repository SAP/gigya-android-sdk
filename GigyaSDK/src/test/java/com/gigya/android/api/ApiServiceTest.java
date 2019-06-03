package com.gigya.android.api;

import com.gigya.android.StaticMockFactory;
import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.api.ApiService;
import com.gigya.android.sdk.api.GigyaApiRequest;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.api.IApiRequestFactory;
import com.gigya.android.sdk.api.IApiService;
import com.gigya.android.sdk.containers.IoCContainer;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.adapter.IRestAdapter;
import com.gigya.android.sdk.network.adapter.IRestAdapterCallback;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
public class ApiServiceTest {

    Config _config;

    @Mock
    IRestAdapter _adapter;

    @Mock
    IApiRequestFactory _reqFactory;

    IoCContainer container = new IoCContainer();

    IApiService apiService;

    @Before
    public void setup() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // Avoid using Android logs.
        GigyaLogger.setDebugMode(false);

        _config = new Config();
        container.bind(Config.class, _config);
        container.bind(IRestAdapter.class, _adapter);
        container.bind(IApiRequestFactory.class, _reqFactory);
        container.bind(IApiService.class, ApiService.class, true);

        // Arrange
        apiService = container.get(IApiService.class);
    }

    @Test
    public void testSuccessfulSend() {

        // Arrange
        _config.setGmid(StaticMockFactory.GMID);

        final String mockJsonResponse = "{\n" +
                "  \"callId\": \"d6e963d1bf5c4d73a010b06fe2182f6c\",\n" +
                "  \"errorCode\": 0,\n" +
                "  \"apiVersion\": 2,\n" +
                "  \"statusCode\": 200,\n" +
                "  \"statusReason\": \"OK\",\n" +
                "  \"time\": \"2019-06-02T06:42:55.678Z\"\n" +
                "}";

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                ((IRestAdapterCallback) invocation.getArgument(2)).onResponse(mockJsonResponse);
                return null;
            }
        }).when(_adapter).send(any(GigyaApiRequest.class), anyBoolean(), any(IRestAdapterCallback.class));

        GigyaApiRequest mockRequest = mock(GigyaApiRequest.class);

        // Act
        apiService.send(mockRequest, false, new ApiService.IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                // Assert
                assertEquals("d6e963d1bf5c4d73a010b06fe2182f6c", response.getCallId());
                assertEquals(0, response.getErrorCode());
                assertEquals(200, response.getStatusCode());
                assertEquals("OK", response.getStatusReason());
                assertEquals("2019-06-02T06:42:55.678Z", response.getTime());
            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                System.out.println("error");
            }
        });
    }

    @Test
    public void testUnsuccessfulSend() {

        // Arrange
        _config.setGmid(StaticMockFactory.GMID);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                ((IRestAdapterCallback) invocation.getArgument(2)).onError(GigyaError.generalError());
                return null;
            }
        }).when(_adapter).send(any(GigyaApiRequest.class), anyBoolean(), any(IRestAdapterCallback.class));

        GigyaApiRequest mockRequest = mock(GigyaApiRequest.class);

        // Act
        apiService.send(mockRequest, false, new ApiService.IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                // Redundant.
            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                assertEquals(400, gigyaError.getErrorCode());
                assertEquals("General error", gigyaError.getLocalizedMessage());
            }
        });
    }

    @Test
    public void testSuccessfulSendWithSuccessfulGetSdkConfig() {

        // Arrange
        final String mockConfigJson = "{\n" +
                "\t\"errorReportRules\": [],\n" +
                "\t\"permissions\": {\n" +
                "\t\t\"facebook\": [\n" +
                "\t\t\t\"user_friends\"],\n" +
                "\t\t\"googleplus\": [\n" +
                "\t\t\t\"profile\",\n" +
                "\t\t\t\"email\",\n" +
                "\t\t\t\"openid\"]\n" +
                "\t},\n" +
                "\t\"appIds\": {\n" +
                "\t\t\"googleplus\": \"977811956095-t72doari7i8iuv9r6qf3kbhh2ns3u7cj.apps.googleusercontent.com\"\n" +
                "\t},\n" +
                "\t\"ids\": {\n" +
                "\t\t\"gmid\": \"KoRxXCZzFKoAFl2jL2WuJMZV4H0nx9NJJ7jxmgJyA7c=\",\n" +
                "\t\t\"ucid\": \"ff3f112d92b657ee\"\n" +
                "\t},\n" +
                "\t\"statusCode\": 200,\n" +
                "\t\"errorCode\": 0,\n" +
                "\t\"statusReason\": \"OK\",\n" +
                "\t\"callId\": \"bd22e624bea844b8827b43e1587711fe\",\n" +
                "\t\"time\": \"2019-06-02T11:03:57.415Z\"\n" +
                "}";

        final String mockJsonResponse = "{\n" +
                "  \"callId\": \"d6e963d1bf5c4d73a010b06fe2182f6c\",\n" +
                "  \"errorCode\": 0,\n" +
                "  \"apiVersion\": 2,\n" +
                "  \"statusCode\": 200,\n" +
                "  \"statusReason\": \"OK\",\n" +
                "  \"time\": \"2019-06-02T06:42:55.678Z\"\n" +
                "}";

        _config.setGmid(null);

        when(_reqFactory.create(anyString(), (Map<String, Object>) any(), anyInt()))
                .thenReturn(mock(GigyaApiRequest.class));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                boolean blocking = invocation.getArgument(1);
                if (blocking) {
                    // First SDK Config request.
                    ((IRestAdapterCallback) invocation.getArgument(2)).onResponse(mockConfigJson);
                } else {
                    // Actual request.
                    assertEquals("KoRxXCZzFKoAFl2jL2WuJMZV4H0nx9NJJ7jxmgJyA7c=", _config.getGmid());
                    assertEquals("ff3f112d92b657ee", _config.getUcid());
                    ((IRestAdapterCallback) invocation.getArgument(2)).onResponse(mockJsonResponse);
                }
                return null;
            }
        }).when(_adapter).send(any(GigyaApiRequest.class), anyBoolean(), any(IRestAdapterCallback.class));

        GigyaApiRequest mockRequest = mock(GigyaApiRequest.class);

        // Act
        apiService.send(mockRequest, false, new ApiService.IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                // Assert
                assertEquals("d6e963d1bf5c4d73a010b06fe2182f6c", response.getCallId());
                assertEquals(0, response.getErrorCode());
                assertEquals(200, response.getStatusCode());
                assertEquals("OK", response.getStatusReason());
                assertEquals("2019-06-02T06:42:55.678Z", response.getTime());
            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                // Redundant.
            }
        });

    }

    @Test
    public void testSuccessfulSendWithUnsuccessfulGetSdkConfig() {

        // Arrange
        final String mockJsonResponse = "{\n" +
                "  \"callId\": \"d6e963d1bf5c4d73a010b06fe2182f6c\",\n" +
                "  \"errorCode\": 0,\n" +
                "  \"apiVersion\": 2,\n" +
                "  \"statusCode\": 200,\n" +
                "  \"statusReason\": \"OK\",\n" +
                "  \"time\": \"2019-06-02T06:42:55.678Z\"\n" +
                "}";

        _config.setGmid(null);

        when(_reqFactory.create(anyString(), (Map<String, Object>) any(), anyInt()))
                .thenReturn(mock(GigyaApiRequest.class));


        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                boolean blocking = invocation.getArgument(1);
                if (blocking) {
                    // First SDK Config request.
                    ((IRestAdapterCallback) invocation.getArgument(2)).onError(GigyaError.generalError());
                }
                return null;
            }
        }).when(_adapter).send(any(GigyaApiRequest.class), anyBoolean(), any(IRestAdapterCallback.class));

        GigyaApiRequest mockRequest = mock(GigyaApiRequest.class);

        // Act
        apiService.send(mockRequest, false, new ApiService.IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                // Redundant.
            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                assertEquals(400, gigyaError.getErrorCode());
                assertEquals("General error", gigyaError.getLocalizedMessage());
            }
        });
    }

}
