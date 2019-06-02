package com.gigya.android.containers;

import com.gigya.android.sdk.containers.IoCContainer;
import org.junit.Before;
import org.junit.Test;

// TODO: #baryo - complete tests

class IoCContainerTest {
    private IoCContainer container;

    @Before
    public void setup() {
        container = new IoCContainer();
    }

    @Test
    public void testGetUnregisteredContract() {}

    @Test
    public void testGetRegisteredInstance() {}

    @Test
    public void testGetRegisteredNoneSingletonClass() {}

    @Test
    public void testGetRegisteredSingletonClass() {}

    @Test
    public void testGetRegisteredClassWithUnregisteredDependencies() {}

    @Test
    public void testGetRegisteredClassWithRegisteredDependencies() {}

    @Test
    public void testGetRegisteredClassWithRegisteredDeepDependencies() {}

    @Test
    public void testGetRegisteredClassWithMultipleConstructors() {/*skip unavailable and use the first available one*/}

    @Test
    public void testGetRegisteredClassWithMultipleAvailableConstructors() {/*use first one*/}

    @Test
    public void testGetRegisteredClassWithOnlyUnavailableConstructors() {/*throw about missing dependencies*/}

    @Test
    public void testCreatingAnInstanceDirectly() {}

    @Test
    public void testChangesOnClonedContainerDoNotEffectOriginal() { }
}