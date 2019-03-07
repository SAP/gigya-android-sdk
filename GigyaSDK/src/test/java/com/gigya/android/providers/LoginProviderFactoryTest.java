package com.gigya.android.providers;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.gigya.android.sdk.GigyaContext;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.providers.LoginProvider;
import com.gigya.android.sdk.providers.LoginProviderFactory;
import com.gigya.android.sdk.providers.provider.FacebookLoginProvider;
import com.gigya.android.sdk.providers.provider.GoogleLoginProvider;
import com.gigya.android.sdk.providers.provider.LineLoginProvider;
import com.gigya.android.sdk.providers.provider.WeChatLoginProvider;
import com.gigya.android.sdk.providers.provider.WebViewLoginProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static junit.framework.TestCase.assertNotNull;
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
    private GigyaContext gigyaContext;

    @Mock
    private ApplicationInfo applicationInfo;

    @Mock
    private Bundle bundle;

    @Mock
    private PackageManager packageManager;

    @Mock
    private GigyaLoginCallback callback;

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
        LoginProvider webProvider = LoginProviderFactory.providerFor(context, gigyaContext, "nonExisting", callback);
        // Assert
        assertNotNull(webProvider);
        assertTrue(webProvider instanceof WebViewLoginProvider);
    }

    @Test
    public void testProviderForFacebook() {
        // Arrange
        when(FacebookLoginProvider.isAvailable(context)).thenReturn(true);
        // Act
        LoginProvider facebookLoginProvider = LoginProviderFactory.providerFor(context, gigyaContext, "facebook", callback);
        // Assert
        assertNotNull(facebookLoginProvider);
        assertTrue(facebookLoginProvider instanceof FacebookLoginProvider);
    }

    @Test
    public void testProviderForGoogle() {
        // Arrange
        when(GoogleLoginProvider.isAvailable(context)).thenReturn(true);
        // Act
        LoginProvider googleLoginProvider = LoginProviderFactory.providerFor(context, gigyaContext, "googleplus", callback);
        // Assert
        assertNotNull(googleLoginProvider);
        assertTrue(googleLoginProvider instanceof GoogleLoginProvider);
    }

    @Test
    public void testProviderForLine() {
        // Arrange
        when(LineLoginProvider.isAvailable(context)).thenReturn(true);
        // Act
        LoginProvider lineLoginProvider = LoginProviderFactory.providerFor(context, gigyaContext, "line", callback);
        // Assert
        assertNotNull(lineLoginProvider);
        assertTrue(lineLoginProvider instanceof LineLoginProvider);
    }

    @Test
    public void testProviderForWeChat() {
        // Arrange
        when(WeChatLoginProvider.isAvailable(context)).thenReturn(true);
        // Act
        LoginProvider weChatLoginProvider = LoginProviderFactory.providerFor(context, gigyaContext, "wechat", callback);
        // Assert
        assertNotNull(weChatLoginProvider);
        assertTrue(weChatLoginProvider instanceof WeChatLoginProvider);
    }
}
