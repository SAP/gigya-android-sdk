package com.gigya.android.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import android.content.Context;

import com.android.volley.toolbox.Volley;
import com.gigya.android.StaticMockFactory;
import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.api.ApiService;
import com.gigya.android.sdk.api.GigyaApiRequest;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.api.IApiRequestFactory;
import com.gigya.android.sdk.api.IApiService;
import com.gigya.android.sdk.containers.IoCContainer;
import com.gigya.android.sdk.encryption.ISecureKey;
import com.gigya.android.sdk.encryption.SessionKey;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.adapter.IRestAdapter;
import com.gigya.android.sdk.network.adapter.IRestAdapterCallback;
import com.gigya.android.sdk.network.adapter.VolleyNetworkProvider;
import com.gigya.android.sdk.persistence.IPersistenceService;
import com.gigya.android.sdk.persistence.PersistenceService;
import com.gigya.android.sdk.session.ISessionService;
import com.gigya.android.sdk.session.SessionService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.reflect.InvocationTargetException;

@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor
        ("com.android.volley.VolleyLog")
@PrepareForTest({Volley.class, VolleyNetworkProvider.class, ApiService.class})
public class ApiServiceTest {

    Config _config;

    @Mock
    Context _context;

    @Mock
    IRestAdapter _adapter;

    @Mock
    IApiRequestFactory _reqFactory;

    private IoCContainer container = new IoCContainer();

    private IApiService apiService;

    @Before
    public void setup() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // Avoid using Android logs.
        mockStatic(Volley.class);
        mockStatic(VolleyNetworkProvider.class);
        mockStatic(System.class);

        when(VolleyNetworkProvider.isAvailable()).thenReturn(false);
        when(Volley.newRequestQueue(_context)).thenReturn(null);
        GigyaLogger.setDebugMode(false);

        _config = new Config();
        container.bind(Context.class, _context);
        container.bind(Config.class, _config);
        container.bind(IPersistenceService.class, PersistenceService.class, false);
        container.bind(ISecureKey.class, SessionKey.class, false);
        container.bind(IApiRequestFactory.class, _reqFactory);
        container.bind(ISessionService.class, SessionService.class, true);
        container.bind(IRestAdapter.class, _adapter);
        container.bind(IApiService.class, ApiService.class, true);

        // Arrange
        apiService = container.get(IApiService.class);
    }

    @Test
    public void testServerOffsetUpdate() {
        // Arrange
        when(System.currentTimeMillis()).thenReturn(1572512740000L);

        final String dateHeaderString = "Thu, 31 Oct 2019 08:20:16 GMT";
        _config.setGmid(StaticMockFactory.GMID);
        _config.setServerOffset(0L);
        final String mockJsonResponse = StaticMockFactory.getMockResponseJson();

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                ((IRestAdapterCallback) invocation.getArgument(2)).onResponse(mockJsonResponse, dateHeaderString);
                return null;
            }
        }).when(_adapter).send(any(GigyaApiRequest.class), anyBoolean(), any(IRestAdapterCallback.class));

        GigyaApiRequest mockRequest = mock(GigyaApiRequest.class);
        when(mockRequest.getApi()).thenReturn("");

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

                assertNotNull(_config.getServerOffset());
                assertEquals(-2724L, (long) _config.getServerOffset());
            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                // Redundant.
                System.out.println("GigyaError");
            }
        });
    }
}
