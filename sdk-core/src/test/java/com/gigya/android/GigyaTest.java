package com.gigya.android;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import android.content.Context;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.ConfigFactory;
import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.containers.IoCContainer;
import com.gigya.android.sdk.providers.IProviderFactory;
import com.gigya.android.sdk.session.ISessionService;
import com.gigya.android.sdk.session.ISessionVerificationService;
import com.gigya.android.sdk.session.SessionInfo;
import com.gigya.android.sdk.session.SessionService;
import com.gigya.android.sdk.ui.IPresenter;
import com.gigya.android.sdk.ui.plugin.GigyaWebBridge;
import com.gigya.android.sdk.ui.plugin.IGigyaWebBridge;
import com.google.gson.Gson;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.reflect.InvocationTargetException;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SessionService.class})
public class GigyaTest {

    @Mock
    Context _context;

    @Mock
    Config _config = new Config().updateWith(StaticMockFactory.API_KEY, StaticMockFactory.API_DOMAIN);

    @Mock
    ConfigFactory _configFactory;

    @Mock
    ISessionService _sessionService;

    @Mock
    IPresenter _presenter;

    @Mock
    ISessionVerificationService _sessionVerificationService;

    @Mock
    IBusinessApiService _businessApiService;

    @Mock
    IProviderFactory _providerFactory;

    @Mock
    IoCContainer _container;

    @InjectMocks
    Gigya gigya;

    @Before
    public void setup() {
    }

    @Test
    public void testGetSession() {
        // Arrange
        final SessionInfo si = new Gson().fromJson(StaticMockFactory.getSessionMock(), SessionInfo.class);
        when(_sessionService.getSession()).thenReturn(si);
        // Act
        final SessionInfo sessionInfo = gigya.getSession();
        // Assert
        assertNotNull(sessionInfo);
        assertEquals(si.getSessionToken(), sessionInfo.getSessionToken());
        assertEquals(si.getSessionSecret(), sessionInfo.getSessionSecret());
    }

    @Test
    public void testIsLoggedIn() {
        // Arrange
        final SessionInfo si = new Gson().fromJson(StaticMockFactory.getSessionMock(), SessionInfo.class);
        when(_sessionService.getSession()).thenReturn(si);
        when(_sessionService.isValid()).thenReturn(true);
        // Act
        final boolean isLoggedIn = gigya.isLoggedIn();
        // Assert
        assertTrue(isLoggedIn);
    }

    @Test
    public void test_createWebBridge() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // Arrange
        GigyaWebBridge mockWb = mock(GigyaWebBridge.class);
        when(_container.get(IGigyaWebBridge.class)).thenReturn(mockWb);
        // Act
        IGigyaWebBridge wb = gigya.createWebBridge();
        // Assert
        assertNotNull(wb);
    }
}
