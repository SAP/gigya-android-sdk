package com.gigya.android.api;

import static org.junit.Assert.assertEquals;
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
import com.gigya.android.sdk.api.RetryDispatcher;
import com.gigya.android.sdk.containers.IoCContainer;
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
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Volley.class, VolleyNetworkProvider.class, ApiService.class})
public class RetryDispatcherTest {


    private IoCContainer container = new IoCContainer();

    Config _config;
    @Mock
    Context _context;
    @Mock
    IRestAdapter _adapter;
    @Mock
    IApiRequestFactory _reqFactory;

    int triesTracker = 0;

    @Before
    public void setup() {
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
        container.bind(IApiRequestFactory.class, _reqFactory);
        container.bind(ISessionService.class, SessionService.class, true);
        container.bind(IRestAdapter.class, _adapter);
        container.bind(IApiService.class, ApiService.class, true);
    }

    @Test
    public void testRetry() {
        // Arrange
        final GigyaApiRequest mockRequest = mock(GigyaApiRequest.class);

        final String mockJsonResponse = StaticMockFactory.mockRequestExpiredErrorJson();
        triesTracker = 2;
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                ((IRestAdapterCallback) invocation.getArgument(2)).onResponse(mockJsonResponse, "Thu, 31 Oct 2019 08:20:16 GMT");
                triesTracker--;
                return null;
            }
        }).when(_adapter).send(any(GigyaApiRequest.class), anyBoolean(), any(IRestAdapterCallback.class));

        // Act
        new RetryDispatcher.Builder(_adapter, _reqFactory)
                .errorCode(GigyaError.Codes.ERROR_REQUEST_HAS_EXPIRED)
                .tries(2)
                .request(mockRequest)
                .handler(new RetryDispatcher.IRetryHandler() {
                    @Override
                    public void onCompleteWithResponse(GigyaApiResponse retryResponse) {
                        System.out.println();

                        // Assert.
                        assertEquals(403002, retryResponse.getErrorCode());
                    }

                    @Override
                    public void onCompleteWithError(GigyaError error) {
                        System.out.println();
                    }

                    @Override
                    public void onUpdateDate(String date) {
                        System.out.println();

                        // Assert.
                        assertEquals("Thu, 31 Oct 2019 08:20:16 GMT", date);
                    }
                }).dispatch();


    }

}
