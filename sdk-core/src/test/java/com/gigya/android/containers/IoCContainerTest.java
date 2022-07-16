package com.gigya.android.containers;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.ConfigFactory;
import com.gigya.android.sdk.containers.IoCContainer;
import com.gigya.android.sdk.encryption.ISecureKey;
import com.gigya.android.sdk.encryption.SessionKey;
import com.gigya.android.sdk.persistence.IPersistenceService;
import com.gigya.android.sdk.persistence.PersistenceService;
import com.gigya.android.sdk.providers.IProviderFactory;
import com.gigya.android.sdk.providers.ProviderFactory;
import com.gigya.android.sdk.session.ISessionService;
import com.gigya.android.sdk.session.SessionService;
import com.gigya.android.sdk.utils.FileUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.reflect.InvocationTargetException;


@RunWith(PowerMockRunner.class)
public class IoCContainerTest {


    private IoCContainer container;

    @Mock
    Context _context;

    @Mock
    Config _config;

    @Mock
    FileUtils _fileUtils;

    @Before
    public void setup() {
        container = new IoCContainer();
    }

    @Test
    public void testGetUnregisteredContract() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // Act
        final ISessionService sessionService = container.get(ISessionService.class);
        // Assert
        assertNull(sessionService);
    }

    @Test
    public void testGetRegisteredInstance() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // Arrange
        container.bind(FileUtils.class, _fileUtils);
        container.bind(ConfigFactory.class, ConfigFactory.class, false);
        // Act
        final ConfigFactory configFactory = container.get(ConfigFactory.class);
        // Assert
        assertNotNull(configFactory);
    }

    @Test
    public void testGetRegisteredNoneSingletonClass() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // Arrange
        container.bind(Context.class, _context);
        container.bind(IPersistenceService.class, PersistenceService.class, false);
        // Act
        final IPersistenceService persistenceService = container.get(IPersistenceService.class);
        final IPersistenceService persistenceServiceClone = container.get(IPersistenceService.class);
        // Assert
        assertNotNull(persistenceService);
        assertNotNull(persistenceServiceClone);
        assertNotEquals(persistenceServiceClone, persistenceService);
    }

    @Test
    public void testGetRegisteredSingletonClass() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // Arrange
        container.bind(Context.class, _context);
        container.bind(IPersistenceService.class, PersistenceService.class, true);
        // Act
        final IPersistenceService persistenceService = container.get(IPersistenceService.class);
        final IPersistenceService persistenceServiceClone = container.get(IPersistenceService.class);
        // Assert
        assertNotNull(persistenceService);
        assertNotNull(persistenceServiceClone);
        assertEquals(persistenceServiceClone, persistenceService);

    }

    @Test(expected = java.util.MissingResourceException.class)
    public void testGetRegisteredClassWithUnregisteredDependencies() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // Arrange
        container.bind(IProviderFactory.class, ProviderFactory.class, true);
        // Act
        container.get(IProviderFactory.class);
    }

    @Test
    public void testGetRegisteredClassWithRegisteredDependencies() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // Arrange
        container.bind(Context.class, _context);
        container.bind(IPersistenceService.class, PersistenceService.class, true);
        // Act
        final IPersistenceService persistenceService = container.get(IPersistenceService.class);
        // Assert
        assertNotNull(persistenceService);
    }

    @Test
    public void testGetRegisteredClassWithRegisteredDeepDependencies() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // Arrange
        container.bind(Context.class, _context);
        container.bind(Config.class, _config);
        container.bind(ISecureKey.class, SessionKey.class, true);
        container.bind(IPersistenceService.class, PersistenceService.class, true);
        container.bind(ISessionService.class, SessionService.class, true);
        // Act
        ISessionService sessionService = container.get(ISessionService.class);
        // Assert
        assertNotNull(sessionService);
    }

    @Test
    public void testGetRegisteredClassWithMultipleConstructors() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // Arrange
        container.bind(Context.class, _context);
        container.bind(TestClassWithMultipleConstructors.class, TestClassWithMultipleConstructors.class, false);
        // Act
        final TestClassWithMultipleConstructors testClass = container.get(TestClassWithMultipleConstructors.class);
        // Assert
        assertNotNull(testClass);
        assertNull(testClass.get_fileUtils());
    }

    @Test
    public void testGetRegisteredClassWithMultipleAvailableConstructors() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // Arrange
        container.bind(FileUtils.class, _fileUtils);
        container.bind(TestClassWithMultipleConstructors.class, TestClassWithMultipleConstructors.class, false);
        // Act
        final TestClassWithMultipleConstructors testClass = container.get(TestClassWithMultipleConstructors.class);
        // Assert
        assertNotNull(testClass);
        assertNotNull(testClass.get_fileUtils());
    }

    @Test(expected = java.util.MissingResourceException.class)
    public void testGetRegisteredClassWithOnlyUnavailableConstructors() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // Arrange
        container.bind(TestClassWithMultipleConstructors.class, TestClassWithMultipleConstructors.class, false);
        // Act
        container.get(TestClassWithMultipleConstructors.class);
    }

    @Test
    public void testCreatingAnInstanceDirectly() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // Arrange
        container.bind(Context.class, _context);
        // Act
        final IPersistenceService persistenceService = container.createInstance(PersistenceService.class);
        // Assert
        assertNotNull(persistenceService);
    }

    @Test(expected = java.util.MissingResourceException.class)
    public void testCreatingAnInstanceWithUnboundDependencies() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // Arrange
        container.bind(Context.class, _context);

        // Act+Assert
        container.createInstance(TestUnboundClass.class);
    }
    @Test
    public void testForceCreatingAnInstanceWithUnboundDependencies() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // Arrange
        container.bind(Context.class, _context);

        // Act+Assert
        assertNotNull(container.createInstance(TestUnboundClass.class, true));
    }

    @Test
    public void testChangesOnClonedContainerDoNotEffectOriginal() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // Arrange
        container.bind(Context.class, _context);
        // Act
        IoCContainer containerClone = container.clone();
        containerClone.bind(IPersistenceService.class, PersistenceService.class, true);
        final IPersistenceService persistenceServiceNull = container.get(IPersistenceService.class);
        final IPersistenceService persistenceServiceNotNull = containerClone.get(IPersistenceService.class);
        // Assert.
        assertNull(persistenceServiceNull);
        assertNotNull(persistenceServiceNotNull);
    }

    @Test(expected = java.lang.NullPointerException.class)
    public void testDisposeContainer() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // Arrange
        container.bind(Context.class, _context);
        // Act
        container.dispose();
        // assert.
        assertNull(container.get(Context.class));
    }


    public static class TestClassWithMultipleConstructors {

        Context _context;
        FileUtils _fileUtils;

        public TestClassWithMultipleConstructors(Context context) {
            _context = context;
        }

        public TestClassWithMultipleConstructors(FileUtils fileUtils) {
            _fileUtils = fileUtils;
        }

        public Context get_context() {
            return _context;
        }

        public FileUtils get_fileUtils() {
            return _fileUtils;
        }
    }

    public static class TestUnboundClass {
        public TestUnboundClass(TestClassWithMultipleConstructors unboundDependency) {}
    }
}