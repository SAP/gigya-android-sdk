package com.gigya.android;

import android.content.Context;

import com.gigya.android.sdk.managers.ApiService;
import com.gigya.android.sdk.managers.IAccountService;
import com.gigya.android.sdk.managers.ISessionService;
import com.gigya.android.sdk.network.GigyaApiRequest;
import com.gigya.android.sdk.network.adapter.INetworkCallbacks;
import com.gigya.android.sdk.network.adapter.IRestAdapter;
import com.gigya.android.sdk.services.Config;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({INetworkCallbacks.class})
public class ApiServiceTest {

    @Mock
    Context mContext;

    @Mock
    Config mConfig;

    @Mock
    IRestAdapter mRestAdapter;

    @Mock
    ISessionService mSessionService;

    @Mock
    IAccountService mAccountService;

    @InjectMocks
    ApiService cApiService;

    @Before
    public void setup() {
        // Config mocks.
        when(mConfig.getApiKey()).thenReturn("mockApiKey");
        when(mConfig.getApiDomain()).thenReturn("us1.gigya.com");
        doNothing().when(mRestAdapter).send((GigyaApiRequest) any(), anyBoolean(), (INetworkCallbacks) any());
    }

    @Test
    public void testSendWithNoParams() {
        // Arrange.
        final String evaluationUrl = "https://accounts.us1.gigya.com/accounts.someApi?ApiKey=mockApiKey&format=json&httpStatusCodes=false&sdk=android_4.0.0&targetEnv=mobile";
        Answer answer = new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                // Assert
                GigyaApiRequest apiRequest = invocation.getArgument(0);
                assertEquals(evaluationUrl, apiRequest.getUrl());
                assertEquals("accounts.someApi", apiRequest.getApi());
                return null;
            }
        };
        doAnswer(answer).when(mRestAdapter).send((GigyaApiRequest) any(), anyBoolean(), (INetworkCallbacks) any());
        // Act.
        cApiService.send("accounts.someApi", new HashMap<String, Object>(), 0, null);
    }
}
