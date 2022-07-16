package com.gigya.android.push;

import com.gigya.android.sdk.containers.IoCContainer;
import com.gigya.android.sdk.push.GigyaNotificationManager;
import com.gigya.android.sdk.push.IGigyaNotificationManager;
import com.gigya.android.sdk.utils.DeviceUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({DeviceUtils.class})
public class GigyaNotificationManagerTest {

    private IGigyaNotificationManager _notificationManager;


    @Before
    public void setUp() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        IoCContainer container = new IoCContainer();
        container.bind(IGigyaNotificationManager.class, GigyaNotificationManager.class, false);

        _notificationManager = container.get(IGigyaNotificationManager.class);
    }

    @Test
    public void testDeviceInfoGeneration() throws JSONException {
        mockStatic(DeviceUtils.class);
        when(DeviceUtils.getManufacturer()).thenReturn("GodPhone");
        when(DeviceUtils.getOsVersion()).thenReturn("2");

        final String mockFcmToken = "mockToken";

        final String deviceInfo = _notificationManager.getDeviceInfo(mockFcmToken);
        JSONObject jsonObject = new JSONObject(deviceInfo);

        assertEquals("GodPhone", jsonObject.get("man"));
        assertEquals("2", jsonObject.get("os"));
        assertEquals("android", jsonObject.get("platform"));
        assertEquals("mockToken", jsonObject.get("pushToken"));
    }
}
