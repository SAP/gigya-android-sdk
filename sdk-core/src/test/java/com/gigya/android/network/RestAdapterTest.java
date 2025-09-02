package com.gigya.android.network;

import android.content.Context;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.api.GigyaApiRequest;
import com.gigya.android.sdk.api.GigyaApiRequestFactory;
import com.gigya.android.sdk.api.IApiRequestFactory;
import com.gigya.android.sdk.containers.IoCContainer;
import com.gigya.android.sdk.network.adapter.IRestAdapter;
import com.gigya.android.sdk.network.adapter.IRestAdapterCallback;
import com.gigya.android.sdk.network.adapter.NetworkProvider;
import com.gigya.android.sdk.network.adapter.RestAdapter;
import com.gigya.android.sdk.persistence.IPersistenceService;
import com.gigya.android.sdk.persistence.PersistenceService;
import com.gigya.android.sdk.session.ISessionService;
import com.gigya.android.sdk.session.SessionService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.lang.reflect.InvocationTargetException;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
public class RestAdapterTest {

    @Mock
    private Context mContext;

    @Mock
    Config mConfig;

    @Mock
    IRestAdapter _restAdapter;

    @Mock
    NetworkProvider _networkProvider;

    private IoCContainer container;

    @Before
    public void setup() {
        container = new IoCContainer();
        container.bind(Context.class, mContext);
        container.bind(Config.class, mConfig);
        container.bind(IPersistenceService.class, PersistenceService.class, false);
        container.bind(IApiRequestFactory.class, GigyaApiRequestFactory.class, false);
        container.bind(ISessionService.class, SessionService.class, true);
        container.bind(IRestAdapter.class, RestAdapter.class, true);
    }

    @Test
    public void testNewInstance() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // Act
        IRestAdapter adapter = container.get(IRestAdapter.class);
        // Assert
        assertNotNull(adapter);
    }

    @Test
    public void testSend() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // Arrange
        IRestAdapter adapter = container.get(IRestAdapter.class);
        Whitebox.setInternalState(adapter, "_networkProvider", _networkProvider);
        doNothing().when(_networkProvider).addToQueue(any(GigyaApiRequest.class), any(IRestAdapterCallback.class));
        doNothing().when(_networkProvider).sendBlocking(any(GigyaApiRequest.class), any(IRestAdapterCallback.class));

        final GigyaApiRequest request = mock(GigyaApiRequest.class);
        final IRestAdapterCallback callback = mock(IRestAdapterCallback.class);

        // Act
        adapter.send(request, false, callback);
    }

    @Test
    public void testSendBlocking() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // Arrange
        IRestAdapter adapter = container.get(IRestAdapter.class);
        Whitebox.setInternalState(adapter, "_networkProvider", _networkProvider);
        doNothing().when(_networkProvider).addToQueue(any(GigyaApiRequest.class), any(IRestAdapterCallback.class));
        doNothing().when(_networkProvider).sendBlocking(any(GigyaApiRequest.class), any(IRestAdapterCallback.class));

        final GigyaApiRequest request = mock(GigyaApiRequest.class);
        final IRestAdapterCallback callback = mock(IRestAdapterCallback.class);

        // Act
        adapter.send(request, true, callback);
    }
}
