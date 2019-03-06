package com.gigya.android.network;

import android.util.Base64;

import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.model.account.SessionInfo;
import com.gigya.android.sdk.network.GigyaApiRequest;
import com.gigya.android.sdk.network.GigyaApiRequestBuilder;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;
import com.gigya.android.sdk.utils.AuthUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;
import java.util.Random;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Base64.class, System.class, Random.class, GigyaApiRequestBuilder.class, AuthUtils.class})
@PowerMockIgnore("javax.crypto.*")
public class GigyaApiRequestBuilderTest {

    private Configuration configuration = new Configuration("dummyApiKey", "us1.gigya.com", 5 * 60000);

    @Mock
    private SessionManager sessionManager;

    @Mock
    private Random mockRandom;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(sessionManager.getSession()).thenReturn(new SessionInfo("mockSecret", "mockToken"));
        when(sessionManager.isValidSession()).thenReturn(true);
        when(sessionManager.getConfiguration()).thenReturn(configuration);
        mockStatic(Base64.class, System.class);
        when(Base64.decode(anyString(), anyInt())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                return java.util.Base64.getMimeDecoder().decode((String) invocation.getArguments()[0]);
            }
        });
        when(Base64.encode(any(byte[].class), anyInt())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                return java.util.Base64.getEncoder().encode((byte[]) invocation.getArguments()[0]);
            }
        });
        when(Base64.encodeToString(any(byte[].class), anyInt())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                return new String(java.util.Base64.getEncoder().encode((byte[]) invocation.getArguments()[0]));
            }
        });
        PowerMockito.when(System.currentTimeMillis()).thenReturn(1545905337000L);
        PowerMockito.whenNew(Random.class).withNoArguments().thenReturn(mockRandom);
        PowerMockito.when(mockRandom.nextInt()).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                return 1;
            }
        });
    }

    @Test
    public void testBuildForGetRequest() {
        // Arrange
        GigyaApiRequestBuilder builder = new GigyaApiRequestBuilder(sessionManager)
                .api("socialize.getSDkConfig")
                .params(new HashMap<String, Object>() {{
                    put("include", "permissions, ids");
                    put("apiKey", configuration.getApiKey());
                }})
                .httpMethod(NetworkAdapter.Method.GET);
        // Act
        GigyaApiRequest request = builder.build();
        // Assert
        assertEquals("https://socialize.us1.gigya.com/socialize.getSDkConfig?apiKey=dummyApiKey&format=json&httpStatusCodes=false&include=permissions%2C%20ids&nonce=1545905337000_1&oauth_token=mockToken&" +
                        "sdk=android_4.0.0&sig=DfljJQXRGfeIQAJ4lQuw5nEGrtU%3D&targetEnv=mobile&timestamp=1545905337",
                request.getUrl());
        assertNull(request.getEncodedParams());
        assertEquals(NetworkAdapter.Method.GET, request.getMethod());
        assertEquals("socialize.getSDkConfig", request.getTag());
    }

    @Test
    public void testBuildForPostRequest() {
        // Arrange
        GigyaApiRequestBuilder builder = new GigyaApiRequestBuilder(sessionManager)
                .api("socialize.getSDkConfig")
                .params(new HashMap<String, Object>() {{
                    put("include", "permissions, ids");
                    put("apiKey", configuration.getApiKey());
                }})
                .httpMethod(NetworkAdapter.Method.POST);
        // Act
        GigyaApiRequest request = builder.build();
        // Assert
        assertEquals("https://socialize.us1.gigya.com/socialize.getSDkConfig", request.getUrl());
        assertNotNull(request.getEncodedParams());
        assertEquals("apiKey=dummyApiKey&format=json&httpStatusCodes=false&include=permissions%2C%20ids&" +
                        "nonce=1545905337000_1&oauth_token=mockToken&sdk=android_4.0.0&sig=ZIt303EWT4EbTkLKCfbvKdeXiuc%3D&targetEnv=mobile&timestamp=1545905337",
                request.getEncodedParams());
        assertEquals(NetworkAdapter.Method.POST, request.getMethod());
        assertEquals("socialize.getSDkConfig", request.getTag());
    }
}
