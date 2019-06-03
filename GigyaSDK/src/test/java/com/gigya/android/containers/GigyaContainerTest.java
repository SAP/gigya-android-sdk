package com.gigya.android.containers;

import android.app.Application;
import android.content.Context;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.ConfigFactory;
import com.gigya.android.sdk.containers.GigyaContainer;
import com.gigya.android.sdk.network.adapter.IRestAdapter;
import com.gigya.android.sdk.utils.FileUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.reflect.InvocationTargetException;

import static junit.framework.TestCase.assertNotNull;

@RunWith(PowerMockRunner.class)
public class GigyaContainerTest {

    @Mock
    Context _context;

    @Mock
    Application _application;

    @Test
    public void testInstantiation() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // Arrange
        GigyaContainer container = new GigyaContainer();
        // Act
        container.bind(Context.class, _context);
        container.bind(Application.class, _application);
        // Assert
        assertNotNull(container.get(FileUtils.class));
        assertNotNull(container.get(Config.class));
        assertNotNull(container.get(ConfigFactory.class));
    }
}
