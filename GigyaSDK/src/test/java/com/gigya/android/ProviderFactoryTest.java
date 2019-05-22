package com.gigya.android;

import android.content.Context;

import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.containers.IoCContainer;
import com.gigya.android.sdk.persistence.IPersistenceService;
import com.gigya.android.sdk.persistence.PersistenceService;
import com.gigya.android.sdk.providers.IProviderFactory;
import com.gigya.android.sdk.providers.ProviderFactory;
import com.gigya.android.sdk.providers.provider.FacebookProvider;
import com.gigya.android.sdk.providers.provider.GoogleProvider;
import com.gigya.android.sdk.providers.provider.IProvider;
import com.gigya.android.sdk.providers.provider.LineProvider;
import com.gigya.android.sdk.providers.provider.Provider;
import com.gigya.android.sdk.providers.provider.WeChatProvider;
import com.gigya.android.sdk.providers.provider.WebLoginProvider;
import com.gigya.android.sdk.utils.FileUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.reflect.InvocationTargetException;

import static com.gigya.android.sdk.GigyaDefinitions.Providers.AMAZON;
import static com.gigya.android.sdk.GigyaDefinitions.Providers.FACEBOOK;
import static com.gigya.android.sdk.GigyaDefinitions.Providers.GOOGLE;
import static com.gigya.android.sdk.GigyaDefinitions.Providers.LINE;
import static com.gigya.android.sdk.GigyaDefinitions.Providers.WECHAT;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({GoogleProvider.class, FacebookProvider.class, LineProvider.class, WeChatProvider.class})
public class ProviderFactoryTest {

    private IoCContainer testContainer = new IoCContainer();

    @Mock
    Context mContext;

    @Mock
    FileUtils mFileUtils;

    @Mock
    IoCContainer mContainer;

    @Mock
    PersistenceService mPersistenceService;

    @Mock
    GigyaLoginCallback mCallback;

    @Mock
    Provider mProvider;

    @Before
    public void setup() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        testContainer.bind(Context.class, mContext);
        testContainer.bind(IoCContainer.class, mContainer);
        testContainer.bind(IPersistenceService.class, mPersistenceService);
        testContainer.bind(FileUtils.class, mFileUtils);
        testContainer.bind(IProviderFactory.class, ProviderFactory.class, false);

        when(mContainer.clone()).thenReturn(mContainer);
        when(mContainer.bind(any(Class.class), any())).thenReturn(mContainer);
        when(mContainer.bind(any(Class.class), any(Class.class), anyBoolean())).thenReturn(mContainer);
        when(mContainer.createInstance(any(Class.class))).then(new Answer<Provider>() {
            @Override
            public Provider answer(InvocationOnMock invocation) {
                return mock((Class<Provider>)invocation.getArguments()[0]);
            }
        });

        mockStatic(FacebookProvider.class, GoogleProvider.class, LineProvider.class, WeChatProvider.class);
    }

    @Test
    public void testGoogle() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // Arrange
        when(GoogleProvider.isAvailable((Context) any())).thenReturn(true);
        // Act
        IProviderFactory factory = testContainer.get(IProviderFactory.class);
        Provider provider = factory.providerFor(GOOGLE, null,mCallback);
        // Assert
        assertNotNull(provider);
        assertTrue(provider instanceof GoogleProvider);
    }

    @Test
    public void testFacebook() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // Arrange
        when(FacebookProvider.isAvailable((FileUtils) any())).thenReturn(true);
        // Act
        IProviderFactory factory = testContainer.get(IProviderFactory.class);
        IProvider provider = factory.providerFor(FACEBOOK, null,mCallback);
        // Assert
        assertNotNull(provider);
        assertTrue(provider instanceof FacebookProvider);
    }


    @Test
    public void testLine() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // Arrange
        when(LineProvider.isAvailable((FileUtils) any())).thenReturn(true);
        // Act
        IProviderFactory factory = testContainer.get(IProviderFactory.class);
        IProvider provider = factory.providerFor(LINE, null,mCallback);
        // Assert
        assertNotNull(provider);
        assertTrue(provider instanceof LineProvider);
    }

    @Test
    public void testWeChat() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // Arrange
        when(WeChatProvider.isAvailable((Context) any(), (FileUtils) any())).thenReturn(true);
        // Act
        IProviderFactory factory = testContainer.get(IProviderFactory.class);
        IProvider provider = factory.providerFor(WECHAT,null, mCallback);
        // Assert
        assertNotNull(provider);
        assertTrue(provider instanceof WeChatProvider);
    }

    @Test
    public void testWeb() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // Act
        IProviderFactory factory = testContainer.get(IProviderFactory.class);
        IProvider provider = factory.providerFor(AMAZON, null, mCallback); // Not available as native.
        // Assert
        assertNotNull(provider);
        assertTrue(provider instanceof WebLoginProvider);
    }

    @Test
    public void testNullName() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // Act
        IProviderFactory factory = testContainer.get(IProviderFactory.class);
        IProvider provider = factory.providerFor(null, null,mCallback); // Not available as native.
        // Assert
        assertNotNull(provider);
        assertTrue(provider instanceof WebLoginProvider);
    }
}
