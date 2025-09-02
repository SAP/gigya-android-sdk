package com.gigya.android;

import android.app.Application;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.containers.IoCContainer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.reflect.InvocationTargetException;

import static junit.framework.TestCase.assertNotNull;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({TextUtils.class})
public class GigyaInitTest {

    @Mock
    Application _application;

    @Mock
    SharedPreferences _sharedPreferences;

    @Before
    public void setup() {
        mockStatic(TextUtils.class);
    }

    @Test
    public void testStaticMethods() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // Act
        Gigya.setApplication(_application);
        final IoCContainer container = Gigya.getContainer();
        // Assert
        assertNotNull(container.get(Application.class));
    }
}
