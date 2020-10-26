package com.gigya.android.sdk.nss

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import com.gigya.android.sdk.Gigya
import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.account.models.GigyaAccount
import com.gigya.android.sdk.nss.bloc.events.NssScreenEvents
import com.gigya.android.sdk.nss.bloc.events.ScreenEventsManager
import com.gigya.android.sdk.nss.engine.NssEngineLifeCycle
import com.gigya.android.sdk.nss.utils.refined
import com.gigya.android.sdk.utils.FileUtils
import java.io.IOException

/**
 * Main NSS builder class.
 * Starting up the Nss engine requires specific parameters and will throw a RuntimeException in the
 * event of:
 * 1 - Malformed JSON markup.
 * 2 - Missing initialRoute parameter.
 */
class Nss private constructor(
        private val engineLifeCycle: NssEngineLifeCycle,
        private val assetPath: String?,
        private val screenSetId: String?,
        private val initialRoute: String?,
        private val lang: String? = "_default",
        private val events: NssEvents<*>?) {

    companion object {

        const val LOG_TAG = "NssBuilder"
    }

    init {
        engineLifeCycle.initializeEngine()
    }

    data class Builder(
            var assetPath: String? = null,
            var screenSetId: String? = null,
            var initialRoute: String? = null,
            var lang: String? = null,
            var events: NssEvents<*>? = null) {

        /**
         * Specify the JSON markup asset file.
         * NOTE: .json suffix is not required.
         */
        fun assetPath(assetPath: String) = apply { this.assetPath = assetPath }

        /**
         * Specify the hosted screen-set id.
         */
        fun screenSetId(id: String) = apply { this.screenSetId = id }

        /**
         * Sets the initial route of the screen-sets.
         */
        fun initialRoute(initialRoute: String) = apply { this.initialRoute = initialRoute }

        /**
         * Allow localization.
         * NOTE: Make sure you have specified the right locale tag in your markup.
         */
        fun lang(language: String) = apply { this.lang = language }

        /**
         * Implement engine general events callback.
         */
        fun <T : GigyaAccount> events(events: NssEvents<T>) = apply {
            this.events = events
            this.events?.let {
                // Injecting the events callback to the singleton view model.
                Gigya.getContainer().get(NssViewModel::class.java).refined<NssViewModel<T>> { viewModel ->
                    viewModel.nssEvents = events
                }
            }
        }

        /**
         * Add a custom screen event. Some of these events are intercepting events.
         * Data can be manipulated before specific engine task is completed.
         */
        fun eventsFor(screenId: String, handler: NssScreenEvents) = apply {
            Gigya.getContainer().get(ScreenEventsManager::class.java).addFor(screenId, handler)
        }

        /**
         * Show the screen-sets engine.
         */
        fun show(launcherContext: Context) = Nss(
                Gigya.getContainer().get(NssEngineLifeCycle::class.java),
                assetPath,
                screenSetId,
                initialRoute,
                lang,
                events)
                .show(launcherContext)
    }

    /**
     * Show native screen-sets.
     * @param launcherContext Root activity context.
     */
    fun show(launcherContext: Context) {
        val data = IgnitionData(assetPath, screenSetId, initialRoute, lang)
        engineLifeCycle.show(launcherContext, data)
    }

}

/**
 * Builder data class.
 * Class is injected to engine activity in order to provide markup initialization data.
 */
data class IgnitionData(var asset: String?, var screenSetId: String?, var initialRoute: String?, var lang: String?) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString()) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(asset)
        parcel.writeString(screenSetId)
        parcel.writeString(initialRoute)
        parcel.writeString(lang)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<IgnitionData> {
        override fun createFromParcel(parcel: Parcel): IgnitionData {
            return IgnitionData(parcel)
        }

        override fun newArray(size: Int): Array<IgnitionData?> {
            return arrayOfNulls(size)
        }
    }
}