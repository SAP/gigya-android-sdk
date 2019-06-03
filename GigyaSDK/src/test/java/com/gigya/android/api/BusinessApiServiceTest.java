package com.gigya.android.api;

import android.content.Context;

import com.gigya.android.StaticMockFactory;
import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.ConfigFactory;
import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.account.GigyaAccountClass;
import com.gigya.android.sdk.account.IAccountService;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.api.ApiService;
import com.gigya.android.sdk.api.BusinessApiService;
import com.gigya.android.sdk.api.GigyaApiRequest;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.api.IApiRequestFactory;
import com.gigya.android.sdk.api.IApiService;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.containers.IoCContainer;
import com.gigya.android.sdk.encryption.ISecureKey;
import com.gigya.android.sdk.interruption.IInterruptionResolverFactory;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.adapter.IRestAdapter;
import com.gigya.android.sdk.network.adapter.RestAdapter;
import com.gigya.android.sdk.persistence.IPersistenceService;
import com.gigya.android.sdk.providers.IProviderFactory;
import com.gigya.android.sdk.session.ISessionService;
import com.gigya.android.sdk.session.SessionInfo;
import com.gigya.android.sdk.utils.FileUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
public class BusinessApiServiceTest {

    @Mock
    Context _context;

    @Mock
    Config _config;

    @Mock
    ISecureKey _secureKey;

    @Mock
    IRestAdapter _adapter;

    @Mock
    IApiRequestFactory _reqFactory;

    @Mock
    ISessionService _sessionService;

    @Mock
    IAccountService _accountService;

    @Mock
    IPersistenceService _persistenceService;

    @Mock
    IApiService _apiService;

    @Mock
    IProviderFactory _providerFactory;

    @Mock
    IInterruptionResolverFactory _interruptionResolverFactory;

    @Mock
    SessionInfo _sessionInfo;


    IoCContainer container;

    Map<String, Object> params;

    @Before
    public void setup() {
        container = new IoCContainer()
                .bind(FileUtils.class, FileUtils.class, true)
                .bind(Config.class, _config)
                .bind(ConfigFactory.class, ConfigFactory.class, false)
                .bind(IRestAdapter.class, _adapter)
                .bind(IApiService.class, _apiService)
                .bind(IApiRequestFactory.class, _reqFactory)
                .bind(ISecureKey.class, _secureKey)
                .bind(IPersistenceService.class, _persistenceService)
                .bind(ISessionService.class, _sessionService)
                .bind(IAccountService.class, _accountService)
                .bind(IProviderFactory.class, _providerFactory)
                .bind(IBusinessApiService.class, BusinessApiService.class, true)
                .bind(IInterruptionResolverFactory.class, _interruptionResolverFactory);
        container.bind(Context.class, _context);
        container.bind(GigyaAccountClass.class, new GigyaAccountClass(GigyaAccount.class));

        params = new HashMap<>();
    }

    private GigyaApiRequest mockRequest() {
        return mock(GigyaApiRequest.class);
    }

    private void mockRequestFactory() {
        when(_reqFactory.create(anyString(), (Map<String, Object>) any(), anyInt())).thenReturn(mockRequest());
    }

    @Test
    public void testNewInstance() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // Act
        IBusinessApiService service = container.get(IBusinessApiService.class);
        // Assert
        assertNotNull(service);
    }

    @Test
    public void testSend() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // Arrange.
        mockRequestFactory();

        // General response.
        final GigyaApiResponse response = new GigyaApiResponse(StaticMockFactory.getMockResponseJson());

        // Act
        IBusinessApiService service = container.get(IBusinessApiService.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                ((ApiService.IApiServiceResponse) invocation.getArgument(2)).onApiSuccess(response);
                return null;
            }
        }).when(_apiService).send(any(GigyaApiRequest.class), anyBoolean(), any(ApiService.IApiServiceResponse.class));
        service.send("accounts.getAccountInfo", params, RestAdapter.POST, GigyaApiResponse.class, new GigyaCallback<GigyaApiResponse>() {
            @Override
            public void onSuccess(GigyaApiResponse obj) {
                // Assert
                assertEquals("d6e963d1bf5c4d73a010b06fe2182f6c", response.getCallId());
                assertEquals(0, response.getErrorCode());
                assertEquals(200, response.getStatusCode());
                assertEquals("OK", response.getStatusReason());
                assertEquals("2019-06-02T06:42:55.678Z", response.getTime());
            }

            @Override
            public void onError(GigyaError error) {
                System.out.println("error");
            }
        });

    }

    @Test
    public void testSendWithTypedResponse() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // Arrange
        mockRequestFactory();

        // Account response.
        final GigyaApiResponse response = new GigyaApiResponse(StaticMockFactory.getMockAccountJson());

        // Act
        IBusinessApiService service = container.get(IBusinessApiService.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                ((ApiService.IApiServiceResponse) invocation.getArgument(2)).onApiSuccess(response);
                return null;
            }
        }).when(_apiService).send(any(GigyaApiRequest.class), anyBoolean(), any(ApiService.IApiServiceResponse.class));
        service.send("accounts.getAccountInfo", params, RestAdapter.POST, GigyaAccount.class, new GigyaCallback<GigyaAccount>() {
            @Override
            public void onSuccess(GigyaAccount response) {
                // Assert
                assertNotNull(response);
                assertEquals("fd30ce9173d24d0d9edce38f7c711b51", response.getCallId());
                assertEquals("06529a7a82e2478a8b008a08dafcf20f", response.getUID());
            }

            @Override
            public void onError(GigyaError error) {
                System.out.println("error");
            }
        });
    }

    @Test
    public void testSendWithForcedError() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // Arrange
        mockRequestFactory();
        // Act
        IBusinessApiService service = container.get(IBusinessApiService.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                ((ApiService.IApiServiceResponse) invocation.getArgument(2)).onApiError(GigyaError.generalError());
                return null;
            }
        }).when(_apiService).send(any(GigyaApiRequest.class), anyBoolean(), any(ApiService.IApiServiceResponse.class));
        service.send("accounts.getAccountInfo", params, RestAdapter.POST, GigyaAccount.class, new GigyaCallback<GigyaAccount>() {
            @Override
            public void onSuccess(GigyaAccount response) {
                // Redundant.
            }

            @Override
            public void onError(GigyaError error) {
                assertEquals(400, error.getErrorCode());
                assertEquals("General error", error.getLocalizedMessage());
            }
        });
    }

    @Test
    public void testSendWithGigyaErrorResponse() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // Arrange
        mockRequestFactory();

        // Gigya error response.
        final GigyaApiResponse response = new GigyaApiResponse(StaticMockFactory.getMockGigyaErorJson());

        // Act
        IBusinessApiService service = container.get(IBusinessApiService.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                ((ApiService.IApiServiceResponse) invocation.getArgument(2)).onApiSuccess(response);
                return null;
            }
        }).when(_apiService).send(any(GigyaApiRequest.class), anyBoolean(), any(ApiService.IApiServiceResponse.class));
        service.send("accounts.getAccountInfo", params, RestAdapter.POST, GigyaApiResponse.class, new GigyaCallback<GigyaApiResponse>() {
            @Override
            public void onSuccess(GigyaApiResponse obj) {
                // Redundant.
            }

            @Override
            public void onError(GigyaError error) {
                System.out.println("error");
                assertEquals(400001, error.getErrorCode());
                assertEquals("d6e963d1bf5c4d73a010b06fe2182f6c", error.getCallId());
            }
        });
    }

}
