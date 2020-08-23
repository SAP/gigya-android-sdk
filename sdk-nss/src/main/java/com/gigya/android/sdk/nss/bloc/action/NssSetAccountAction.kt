package com.gigya.android.sdk.nss.bloc.action

import android.annotation.SuppressLint
import android.util.Base64
import com.gigya.android.sdk.GigyaCallback
import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.account.models.GigyaAccount
import com.gigya.android.sdk.api.GigyaApiResponse
import com.gigya.android.sdk.api.IBusinessApiService
import com.gigya.android.sdk.interruption.IPendingRegistrationResolver
import com.gigya.android.sdk.network.GigyaError
import com.gigya.android.sdk.network.adapter.RestAdapter
import com.gigya.android.sdk.nss.bloc.flow.INssFlowDelegate
import com.gigya.android.sdk.nss.bloc.flow.NssResolver
import com.gigya.android.sdk.nss.utils.guard
import com.gigya.android.sdk.nss.utils.refined
import com.gigya.android.sdk.nss.utils.serializeToMap
import io.flutter.plugin.common.MethodChannel

class NssSetAccountAction<T : GigyaAccount>(private val businessApi: IBusinessApiService<T>) : NssAction<T>(businessApi) {

    companion object {
        const val LOG_TAG = "NssAccountFlow"

        const val includeAll = "identities-active , identities-all , identities-global , loginIDs ," +
                " emails, profile, data, password, isLockedOut, lastLoginLocation, regSource, irank, rba, subscriptions, userInfo, preferences"
        const val extraProfileFieldsAll = "languages, address, phones, education, educationLevel," +
                " honors, publications,  patents, certifications, professionalHeadline, bio, industry," +
                " specialties, work, skills, religion, politicalView, interestedIn, relationshipStatus," +
                " hometown, favorites, followersCount, followingCount, username, name, locale, verified, timezone, likes, samlData"

        const val setProfilePhoto = "setProfilePhoto"

        const val SET_PROFILE_ENGINE_GENERAL_ERROR = "500"
        const val SET_PROFILE_ENGINE_SIZE_ERROR = "413004"
    }

    /**
     * Set temporary profile photo flag.
     */
    var publishPhotoOnSubmit = false

    /**
     * Action may use a resolver as part of a registration/login flow.
     */
    var pendingRegistrationResolver: IPendingRegistrationResolver? = null

    override fun initialize(result: MethodChannel.Result) {
        val params = mutableMapOf<String, Any>(
                "include" to includeAll,
                "extraProfileFields" to extraProfileFieldsAll
        )
        flowDelegate?.getResolver()?.let { nssResolver ->
            nssResolver.refined<NssResolver<IPendingRegistrationResolver>> {
                pendingRegistrationResolver = it.resolver
                params["regToken"] = pendingRegistrationResolver!!.regToken
            }
        }

        businessApi.getAccount(params, object : GigyaCallback<T>() {

            override fun onSuccess(obj: T) {
                val map = obj.serializeToMap(gson).guard {
                    throw RuntimeException("Failed to serialize account object")
                }
                result.success(map)
            }

            override fun onError(error: GigyaError?) {
                error?.let {
                    GigyaLogger.error(LOG_TAG, "initialize() failed with: ${it.localizedMessage}")
                    result.error(it.errorCode.toString(), it.localizedMessage, it.data)
                }
            }

        })
    }

    override fun onNext(method: String, arguments: MutableMap<String, Any>?) {
        flowDelegate.guard {
            GigyaLogger.error(LOG_TAG, "Action flow delegate not set")
        }

        // Run super first to allow general action handling.
        super.onNext(method, arguments)

        when (method) {
            submit -> {
                setAccount(arguments)
            }
            setProfilePhoto -> {
                arguments?.let {
                    setProfilePhoto(it["data"] as ByteArray)
                }
            }
        }
    }

    var profileImageResult: MethodChannel.Result? = null

    /**
     * Submit account form. Update the account data.
     */
    private fun setAccount(arguments: MutableMap<String, Any>?) {
        flattenArguments(arguments)
        if (pendingRegistrationResolver != null) {
            // Resolve interruption as part of a registration/login flow.
            pendingRegistrationResolver?.setAccount(arguments!!)
        } else {
            flowDelegate!!.refined<INssFlowDelegate<T>> {

                // Apply default set account action using the business api.
                businessApi.setAccount(arguments, it.getMainFlowCallback())

                // If a temporary profile photo is available, publish it.
                if (publishPhotoOnSubmit) {
                    publishProfilePhoto()
                }
            }
        }
    }


    /**
     * Set new profile photo.
     * Endpoint is used without the "publish" tag.
     * In order to publish the screen form will need to be submitted.
     */
    @SuppressLint("NewApi")
    fun setProfilePhoto(data: ByteArray) {
        // Need to check the size of the image. We cant allow file upload that exceeds 6 MB.
        val imageSize = data.size / (1024 * 1025)
        if (imageSize >= 6) {
            profileImageResult?.error(SET_PROFILE_ENGINE_SIZE_ERROR,
                    "Image size exceeds 6 MB", null)
            return
        }
        // Encode & send image.
        val encoded = Base64.encodeToString(data, Base64.NO_PADDING and Base64.NO_WRAP)
        val params = mapOf<String, Any>("photoBytes" to encoded)
        businessApi.send(
                "accounts.setProfilePhoto",
                params,
                RestAdapter.HttpMethod.POST.intValue(),
                GigyaApiResponse::class.java, object : GigyaCallback<GigyaApiResponse>() {
            override fun onSuccess(obj: GigyaApiResponse?) {
                GigyaLogger.debug(LOG_TAG, "setProfilePhoto: success")
                publishPhotoOnSubmit = true
                profileImageResult?.success(data)
            }

            override fun onError(error: GigyaError?) {
                GigyaLogger.debug(LOG_TAG, "setProfilePhoto: failed")
                publishPhotoOnSubmit = false
                profileImageResult?.error(SET_PROFILE_ENGINE_GENERAL_ERROR,
                        "Failed to update profile photo", null)
            }
        })
    }


    /**
     * Publish requested profile photo along side with the submission form.
     * Currently no result/error handling is relevant.
     */
    private fun publishProfilePhoto() {
        businessApi.send(
                "accounts.publishProfilePhoto",
                null,
                RestAdapter.HttpMethod.POST.intValue(),
                GigyaApiResponse::class.java, object : GigyaCallback<GigyaApiResponse>() {
            override fun onSuccess(obj: GigyaApiResponse?) {
                GigyaLogger.debug(LOG_TAG, "publishProfilePhoto: success")
                publishPhotoOnSubmit = false
            }

            override fun onError(error: GigyaError?) {
                GigyaLogger.debug(LOG_TAG, "publishProfilePhoto: failed")
                publishPhotoOnSubmit = false
            }
        })
    }
}
