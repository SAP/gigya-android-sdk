package com.gigya.android.network;

import android.content.Context;

import com.android.volley.toolbox.Volley;
import com.gigya.android.sdk.network.adapter.IRestAdapter;
import com.gigya.android.sdk.network.adapter.RestAdapter;
import com.gigya.android.sdk.network.adapter.VolleyNetworkProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor
        ("com.android.volley.VolleyLog")
@PrepareForTest({Volley.class, VolleyNetworkProvider.class})
public class RestAdapterTest {

    @Mock
    private Context mContext;

    private IRestAdapter cRestAdapter;

    @Before
    public void setup() {
        mockStatic(Volley.class);
        when(Volley.newRequestQueue(mContext)).thenReturn(null);
    }

    @Test
    public void testNewInstance() {
        // Act
        cRestAdapter = new RestAdapter(mContext);
        // Assert
        assertNotNull(cRestAdapter);
    }

    @Test
    public void testNewVolleyInstance() {
        // Act
        cRestAdapter = new RestAdapter(mContext);
        // Assert
        assertNotNull(cRestAdapter);
        assertEquals("VolleyNetworkProvider", cRestAdapter.getProviderType());
    }

    @Test
    public void testNewHttpInstance() {
        // Arrange
        mockStatic(VolleyNetworkProvider.class);
        when(VolleyNetworkProvider.isAvailable()).thenReturn(false);
        // Act
        cRestAdapter = new RestAdapter(mContext);
        // Assert
        assertNotNull(cRestAdapter);
        assertEquals("HttpNetworkProvider", cRestAdapter.getProviderType());
    }

    @Test
    public void testSend() {

    }

    @Test
    public void testSendBlocking() {

    }

    @Test
    public void testGetProviderType() {

    }
}
