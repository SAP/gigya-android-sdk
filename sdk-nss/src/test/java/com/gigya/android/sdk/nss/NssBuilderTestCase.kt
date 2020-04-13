package com.gigya.android.sdk.nss

import com.gigya.android.sdk.utils.FileUtils
import org.apache.commons.io.IOUtils
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.powermock.api.mockito.PowerMockito

class NssBuilderTestCase : NssTestCase() {

    @Rule
    var expectedException: ExpectedException = ExpectedException.none()

    @Test
    @Throws(RuntimeException::class)
    fun testNssBuilder_FailWitJSONParseException() {
        PowerMockito.doNothing().`when`(engineLifeCycle)?.show(context!!, null)
        PowerMockito.`when`(FileUtils.assetJsonFileToString(context, "whatever")).thenReturn(null)

        expectedException.expect(RuntimeException::class.java)
        expectedException.expectMessage("Failed to parse JSON File from assets folder")

        // Load JSON from asset will result in null and throw the relevant exception.
        Nss.Builder().assetPath("whatever").show(context!!)
    }

    @Test
    @Throws(RuntimeException::class)
    fun testNssBuilder_FailWithMarkupSerializeException() {
        PowerMockito.doNothing().`when`(engineLifeCycle)?.show(context!!, null)
        PowerMockito.`when`(FileUtils.assetJsonFileToString(context, "whatever")).thenReturn("{}")

        expectedException.expect(RuntimeException::class.java)
        expectedException.expectMessage("Markup scheme incorrect - missing \"markup\" field")

        Nss.Builder().assetPath("whatever").show(context!!)
    }

    @Test
    @Throws(RuntimeException::class)
    fun testNssBuilder_FailWithNoAvailableInitialRouteException() {
        val json: String = IOUtils.toString(this.javaClass.classLoader?.getResourceAsStream(
                "nss_markup_mock_no_initial_route.json"),
                "UTF-8")

        PowerMockito.doNothing().`when`(engineLifeCycle)?.show(context!!, null)
        PowerMockito.`when`(FileUtils.assetJsonFileToString(context, "whatever")).thenReturn(json)

        expectedException.expect(RuntimeException::class.java)
        expectedException.expectMessage("Markup scheme incorrect - initial route must be provided")

        Nss.Builder().assetPath("whatever").show(context!!)
    }
}