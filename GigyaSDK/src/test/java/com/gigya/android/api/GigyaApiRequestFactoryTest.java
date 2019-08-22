package com.gigya.android.api;

import android.content.Context;

import com.gigya.android.StaticMockFactory;
import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.api.GigyaApiRequest;
import com.gigya.android.sdk.api.GigyaApiRequestFactory;
import com.gigya.android.sdk.api.IApiRequestFactory;
import com.gigya.android.sdk.containers.IoCContainer;
import com.gigya.android.sdk.encryption.ISecureKey;
import com.gigya.android.sdk.network.adapter.RestAdapter;
import com.gigya.android.sdk.persistence.IPersistenceService;
import com.gigya.android.sdk.persistence.PersistenceService;
import com.gigya.android.sdk.session.ISessionService;
import com.gigya.android.sdk.session.SessionInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
public class GigyaApiRequestFactoryTest {

    @Mock
    Context _context;

    @Mock
    Config _config;

    @Mock
    ISecureKey _secureKey;

    @Mock
    ISessionService _sessionService;

    @Mock
    SessionInfo _sessionInfo;

    IoCContainer container = new IoCContainer();

    Map<String, Object> params;

    @Before
    public void setup() {
        container.bind(Context.class, _context);
        container.bind(Config.class, _config);
        container.bind(IPersistenceService.class, PersistenceService.class, true);
        container.bind(ISecureKey.class, _secureKey);
        container.bind(ISessionService.class, _sessionService);
        container.bind(IApiRequestFactory.class, GigyaApiRequestFactory.class, true);

        params = new HashMap<>();
    }

    @Test
    public void testCreatePOSTWithValidSession() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // Arrange
        when(_config.getApiDomain()).thenReturn("us1.gigya.com");
        when(_sessionService.isValid()).thenReturn(true);
        when(_sessionService.getSession()).thenReturn(_sessionInfo);
        when(_sessionInfo.getSessionToken()).thenReturn(StaticMockFactory.getMockToken());
        // Act
        final IApiRequestFactory factory = container.get(IApiRequestFactory.class);
        GigyaApiRequest request = factory.create("TestAPI", params, RestAdapter.POST);
        // Assert
        assertNotNull(request);
        assertEquals("TestAPI", request.getApi());
        assertEquals("TestAPI", request.getTag());
        assertEquals("https://TestAPI.us1.gigya.com/TestAPI", request.getUrl());
        assertEquals(RestAdapter.POST, request.getMethod());
        assertNotNull(request.getEncodedParams());
    }

    @Test
    public void testCreateGETWithValidSession() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // Arrange
        when(_config.getApiDomain()).thenReturn("us1.gigya.com");
        when(_config.getApiKey()).thenReturn(StaticMockFactory.API_KEY);
        when(_sessionService.isValid()).thenReturn(false);
        when(_sessionService.getSession()).thenReturn(_sessionInfo);
        when(_sessionInfo.getSessionToken()).thenReturn(StaticMockFactory.getMockToken());
        // Act
        final IApiRequestFactory factory = container.get(IApiRequestFactory.class);
        GigyaApiRequest request = factory.create("TestAPI", params, RestAdapter.POST);
        // Assert
        assertNotNull(request);
        assertEquals("TestAPI", request.getApi());
        assertEquals(RestAdapter.POST, request.getMethod());
        assertEquals("ApiKey=3_eP-lTMvtVwgjBCKCWPgYfeWH4xVkD5Rga15I7aoVvo-S_J5ZRBLg9jLDgJvDJZag&format=json&httpStatusCodes=false&sdk=" + Gigya.VERSION + "&targetEnv=mobile", request.getEncodedParams());
    }


    @Test
    public void testCreatePOSTWithoutValidSession() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // Arrange
        when(_config.getApiDomain()).thenReturn("us1.gigya.com");
        when(_sessionService.isValid()).thenReturn(true);
        when(_sessionService.getSession()).thenReturn(_sessionInfo);
        when(_sessionInfo.getSessionToken()).thenReturn(StaticMockFactory.getMockToken());
        // Act
        final IApiRequestFactory factory = container.get(IApiRequestFactory.class);
        GigyaApiRequest request = factory.create("TestAPI", params, RestAdapter.GET);
        // Assert
        assertNotNull(request);
        assertEquals("TestAPI", request.getApi());
        assertEquals(RestAdapter.GET, request.getMethod());
        assertNull(request.getEncodedParams());
        assertTrue(request.getUrl().contains("https://TestAPI.us1.gigya.com/TestAPI?format=json&httpStatusCodes=false"));
    }

    @Test
    public void testCreateGETWithoutValidSession() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // Arrange
        when(_config.getApiDomain()).thenReturn("us1.gigya.com");
        when(_config.getApiKey()).thenReturn(StaticMockFactory.API_KEY);
        when(_sessionService.isValid()).thenReturn(false);
        when(_sessionService.getSession()).thenReturn(_sessionInfo);
        when(_sessionInfo.getSessionToken()).thenReturn(StaticMockFactory.getMockToken());
        // Act
        final IApiRequestFactory factory = container.get(IApiRequestFactory.class);
        GigyaApiRequest request = factory.create("TestAPI", params, RestAdapter.GET);
        // Assert
        assertNotNull(request);
        assertEquals("TestAPI", request.getApi());
        assertEquals(RestAdapter.GET, request.getMethod());
        assertNull(request.getEncodedParams());
        assertTrue(request.getUrl().contains("ApiKey=" + StaticMockFactory.API_KEY));
    }

}
