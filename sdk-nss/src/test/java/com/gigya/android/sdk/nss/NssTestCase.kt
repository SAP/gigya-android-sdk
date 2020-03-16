package com.gigya.android.sdk.nss

import android.content.Context
import com.gigya.android.sdk.containers.GigyaContainer
import com.gigya.android.sdk.nss.engine.NssEngineLifeCycle
import com.gigya.android.sdk.utils.FileUtils
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
@PrepareForTest(FileUtils::class, GigyaNss::class, NssEngineLifeCycle::class, NssViewModel::class, GigyaContainer::class)
abstract class NssTestCase {

    @Mock
    protected val context: Context? = null

    @Mock
    protected val engineLifeCycle: NssEngineLifeCycle? = null

    @Mock
    protected val container: GigyaContainer? = null

    @Mock
    protected val viewModel: NssViewModel<*>? = null

    @Before
    fun prepareMocks() {
        MockitoAnnotations.initMocks(this)
        PowerMockito.mockStatic(FileUtils::class.java, GigyaNss::class.java)

        val field = PowerMockito.field(GigyaNss::class.java, "dependenciesContainer")
        field.set(GigyaNss::class.java, container)

        PowerMockito.`when`(container?.get(NssViewModel::class.java)).thenReturn(viewModel)
        PowerMockito.`when`(container?.get(NssEngineLifeCycle::class.java)).thenReturn(engineLifeCycle)

        PowerMockito.doNothing().`when`(engineLifeCycle)?.initializeEngine()
    }

}