package com.gigya.android;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.gigya.android.sdk.login.LoginProvider;
import com.gigya.android.sdk.login.LoginProviderFactory;
import com.gigya.android.sdk.login.provider.FacebookLoginProvider;
import com.gigya.android.sdk.login.provider.GoogleLoginProvider;
import com.gigya.android.sdk.login.provider.LineLoginProvider;
import com.gigya.android.sdk.login.provider.WeChatLoginProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({FacebookLoginProvider.class, GoogleLoginProvider.class, LineLoginProvider.class, WeChatLoginProvider.class, ApplicationInfo.class, PackageManager.class, Bundle.class})
public class LoginProviderFactoryTest {

    @Mock
    private Context context;

    @Mock
    private ApplicationInfo applicationInfo;

    @Mock
    private Bundle bundle;

    @Mock
    private PackageManager packageManager;

    @Mock
    private LoginProvider.LoginProviderCallbacks loginProviderCallbacks;

    @Mock
    private LoginProvider.LoginProviderTrackerCallback loginProviderTrackerCallback;

    @Before
    public void setup() throws PackageManager.NameNotFoundException {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(FacebookLoginProvider.class, GoogleLoginProvider.class, LineLoginProvider.class, WeChatLoginProvider.class, Bundle.class);
        /// Android specific mocks.
        when(context.getPackageManager()).thenReturn(packageManager);
        when(context.getPackageName()).thenReturn("");
        when(packageManager.getApplicationInfo(anyString(), anyInt())).thenReturn(applicationInfo);
        applicationInfo.metaData = bundle;
        when(bundle.get(anyString())).thenReturn(null);
    }

    @Test
    public void testProviderForWithInvalidProviderName() {
        // Act
        LoginProvider nullProvider = LoginProviderFactory.providerFor(context, "nonExisting", loginProviderCallbacks, loginProviderTrackerCallback);
        // Assert
        assertNull(nullProvider);
    }

    @Test
    public void testProviderForFacebook() {
        // Arrange
        when(FacebookLoginProvider.isAvailable(context)).thenReturn(true);
        // Act
        LoginProvider facebookLoginProvider = LoginProviderFactory.providerFor(context, "facebook", loginProviderCallbacks, loginProviderTrackerCallback);
        // Assert
        assertNotNull(facebookLoginProvider);
        assertTrue(facebookLoginProvider instanceof FacebookLoginProvider);
    }

    @Test
    public void testProviderForGoogle() {
        // Arrange
        when(GoogleLoginProvider.isAvailable(context)).thenReturn(true);
        // Act
        LoginProvider googleLoginProvider = LoginProviderFactory.providerFor(context, "googleplus", loginProviderCallbacks, loginProviderTrackerCallback);
        // Assert
        assertNotNull(googleLoginProvider);
        assertTrue(googleLoginProvider instanceof GoogleLoginProvider);
    }

    @Test
    public void testProviderForLine() {
        // Arrange
        when(LineLoginProvider.isAvailable(context)).thenReturn(true);
        // Act
        LoginProvider lineLoginProvider = LoginProviderFactory.providerFor(context, "line", loginProviderCallbacks, loginProviderTrackerCallback);
        // Assert
        assertNotNull(lineLoginProvider);
        assertTrue(lineLoginProvider instanceof LineLoginProvider);
    }

    @Test
    public void testProviderForWeChat() {
        // Arrange
        when(WeChatLoginProvider.isAvailable(context)).thenReturn(true);
        // Act
        LoginProvider weChatLoginProvider = LoginProviderFactory.providerFor(context, "wechat", loginProviderCallbacks, loginProviderTrackerCallback);
        // Assert
        assertNotNull(weChatLoginProvider);
        assertTrue(weChatLoginProvider instanceof WeChatLoginProvider);
    }
}
