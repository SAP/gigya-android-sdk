package com.gigya.android.sdk.network.adapter

import android.os.Handler
import android.os.Looper
import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.api.GigyaApiHttpRequest
import com.gigya.android.sdk.api.GigyaApiRequest
import com.gigya.android.sdk.api.IApiRequestFactory
import com.gigya.android.sdk.network.GigyaError
import okhttp3.Call
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.net.HttpURLConnection
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class OkHttpNetworkAdapter(requestFactory: IApiRequestFactory?) : NetworkProvider(requestFactory) {

    companion object {
        @JvmStatic
        fun isAvailable(): Boolean {
            return try {
                // OKHttpAdapter requires both client & logging dependencies to be present.
                var required1 = Class.forName("okhttp3.OkHttpClient")
                var required2 = Class.forName("okhttp3.logging.HttpLoggingInterceptor")
                true
            } catch (ex: Exception) {
                false
            }
        }
    }

    private val _queue: Queue<OkHttpTask> = ConcurrentLinkedQueue()
    private val client = NetworkClient()

    override fun addToQueue(
        request: GigyaApiRequest,
        networkCallbacks: IRestAdapterCallback
    ) {
        if (_blocked) {
            // Add request to queue.
            _queue.add(
                OkHttpTask(OkHttpAsyncTask(networkCallbacks, client), request)
            )
            return
        }
        val signedRequest = _requestFactory.sign(request)
        OkHttpAsyncTask(networkCallbacks, client).execute(signedRequest)
    }

    override fun addToQueueUnsigned(
        request: GigyaApiRequest,
        networkCallbacks: IRestAdapterCallback
    ) {
        // Send the request here.
        val unsignedRequest = _requestFactory.unsigned(request)
        OkHttpAsyncTask(networkCallbacks, client).execute(unsignedRequest)
    }

    override fun sendBlocking(
        request: GigyaApiRequest,
        networkCallbacks: IRestAdapterCallback
    ) {
        // Send the request here.
        val signedRequest = _requestFactory.sign(request)
        OkHttpAsyncTask(networkCallbacks, client).execute(signedRequest)
        _blocked = true
    }

    override fun cancel(tag: String?) {
        if (tag == null) {
            _queue.clear()
            // Unable to cancel already sent requests.
        }
        if (!_queue.isEmpty()) {
            val it: MutableIterator<*> = _queue.iterator()
            while (it.hasNext()) {
                val task = it.next() as OkHttpTask
                val requestTag = task.request.tag
                if (requestTag == tag) {
                    it.remove()
                }
            }
        }
    }

    override fun release() {
        super.release()
        if (_queue.isEmpty()) {
            return
        }
        while (!_queue.isEmpty()) {
            val queued: OkHttpTask = _queue.poll() as OkHttpTask

            if (!queued.request.params.containsKey("regToken")) {
                val signedRequest = _requestFactory.sign(queued.request)
                queued.task.execute(signedRequest)
            } else {
                val unsignedRequest = _requestFactory.unsigned(queued.request)
                queued.task.execute(unsignedRequest)
            }
        }
    }

}

data class OkHttpTask(
    var task: OkHttpAsyncTask,
    var request: GigyaApiRequest
)

data class Result(
    val code: Int,
    val result: String?,
    val date: String?
)

class NetworkClient {

    internal companion object {
        const val DEFAULT_TIMEOUT: Int = 30
    }

    private val okHttpClient: OkHttpClient

    init {
        val builder = OkHttpClient.Builder()

        builder.connectTimeout(DEFAULT_TIMEOUT.toLong(), TimeUnit.SECONDS)
        builder.readTimeout(DEFAULT_TIMEOUT.toLong(), TimeUnit.SECONDS)

        // Add network logging if set to allow (default set to false).
        if (GigyaLogger.isDebug()) {
            builder.addInterceptor(
                HttpLoggingInterceptor()
                    .setLevel(HttpLoggingInterceptor.Level.BODY)
            )
        }
        okHttpClient = builder.build()
    }

    fun newCall(request: Request): Call {
        return okHttpClient.newCall(request);
    }
}

open class OkHttpAsyncTask(
    val callback: IRestAdapterCallback,
    private val client: NetworkClient,
) {

    private var executor: ExecutorService? = null
    private var handler: Handler? = null

    companion object {
        const val LOG_TAG = "OkHttpAsyncTask"
        const val REQUEST_CONTENT_TYPE = "application/x-www-form-urlencoded"
    }

    init {
        executor = Executors.newSingleThreadExecutor { r ->
            val t = Thread(r)
            t.isDaemon = true
            return@newSingleThreadExecutor t
        }
    }

    private fun getHandler(): Handler? {
        if (handler == null) {
            synchronized(OkHttpAsyncTask::class.java) {
                handler = Handler(Looper.getMainLooper())
            }
        }
        return handler
    }

    fun execute(request: GigyaApiHttpRequest) {
        executor?.execute {
            GigyaLogger.debug(LOG_TAG, "Executor: execute request with ${request.url}")
            val result = doInBackground(request)
            getHandler()?.post {
                GigyaLogger.debug(LOG_TAG, "Executor: post execute request with ${request.url}")
                result?.let { onPostExecute(it) }
                shutDown()
            }
        }
    }

    private fun doInBackground(request: GigyaApiHttpRequest): Result {
        // Make OkHttp call.
        val builder = Request.Builder()
        builder.url(request.url)
        request.headers?.let {
            val headers = it
            if (!it.containsKey("User-Agent")) {
                // Add default device user agent. If User-Agent was specifically set in the request,
                // it will not be added.
                it["User-Agent"] = System.getProperty("http.agent")
            }
            builder.headers(it.toHeaders())
        }
        request.encodedParams?.let {
            val data = it.toByteArray()
            builder.post(data.toRequestBody())
        }
        builder.header("Content-Type", REQUEST_CONTENT_TYPE)
        val okHttpRequest = builder.build()
        val call = client.newCall(okHttpRequest)
        return try {
            val response = call.execute()
            val responseCode = response.code
            val responseBody = response.body?.string()
            val responseDate = response.headers["date"]
            Result(responseCode, responseBody, responseDate)
        } catch (ex: Exception) {
            ex.printStackTrace()
            GigyaLogger.error(LOG_TAG, "Call execution exception with ${ex.message}")
            Result(400106, null, null)
        }
    }

    private fun onPostExecute(result: Result) {
        // Process response.
        val badRequest: Boolean = result.code >= HttpURLConnection.HTTP_BAD_REQUEST
        if (badRequest) {
            val noNetworkRequest = result.code == 400106
            if (noNetworkRequest) {
                val noNetworkError = GigyaError(
                    400106,
                    "User is not connected to the required network or to any network",
                    null
                )
                GigyaLogger.debug("GigyaApiResponse", "No network error")
                callback.onError(noNetworkError)
                return
            }

            // Generate gigya error.
            val gigyaError = GigyaError(result.code, result.result!!, null)
            callback.onError(gigyaError)
            return
        }
        callback.onResponse(result.result, result.date)
    }

    private fun shutDown() {
        if (executor != null) {
            executor!!.shutdownNow()
        }
    }
}