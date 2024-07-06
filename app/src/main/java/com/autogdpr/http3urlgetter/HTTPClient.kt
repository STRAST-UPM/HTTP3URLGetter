package com.autogdpr.http3urlgetter

import android.content.Context
import android.util.Log
import com.google.android.gms.net.CronetProviderInstaller
import org.chromium.net.CronetEngine
import org.chromium.net.CronetException
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo
import java.io.ByteArrayOutputStream
import java.net.URI
import java.nio.ByteBuffer
import java.util.concurrent.Executors

class CronetConfig(private val applicationContext: Context) {

    private var cronetEngine: CronetEngine? = null

    fun getCronetEngine(): CronetEngine {
        cronetEngine?.let { return it }

        // Instala el proveedor Cronet
        CronetProviderInstaller.installProvider(applicationContext)

        // Crea cronetEngine
        cronetEngine = CronetEngine.Builder(applicationContext)
            .enableQuic(true)
            .build()
        return cronetEngine!!
    }

    fun shutdown() {
        cronetEngine?.shutdown()
    }
}

class HTTPClient(private val applicationContext: Context) {

    private val cronetConfig = CronetConfig(applicationContext)
    private val cronetEngine = cronetConfig.getCronetEngine()
    private var url: String = ""
    private var responseBody = ""
    private var protocol = ""

    fun printCustomStackTrace() {
        var traceClasses = arrayListOf<String>()
        val traces = Thread.currentThread().getStackTrace()
        traces.forEach { traceClasses.add(it.getClassName().toString()) }
        Log.d("StackTrace", traceClasses.toString())
    }

    fun performGetRequest(url: String): Pair<String, String> {
        this.url = url
        val requestBuilder = cronetEngine.newUrlRequestBuilder(
            url,
            MyUrlRequestCallback(),
            Executors.newSingleThreadExecutor()
        ).addHeader("Quic-Protocol", "quic/h3")

        val request = requestBuilder.build()
        request.start()

        // Este método probablemente debería devolver algo asíncrono
        // En lugar de una cadena de resultados inmediata
        return Pair(protocol, responseBody)
    }

    inner class MyUrlRequestCallback : UrlRequest.Callback() {
        private val buffer = ByteArrayOutputStream()

        override fun onResponseStarted(request: UrlRequest?, info: UrlResponseInfo?) {
            Log.d("HTTPClient", "Response started")
            val httpStatusCode = info?.httpStatusCode
            if (httpStatusCode == 200) {
                // The request was fulfilled. Start reading the response.
                request?.read(ByteBuffer.allocateDirect(102400))
            } else if (httpStatusCode == 503) {
                // The service is unavailable. You should still check if the request
                // contains some data.
                request?.read(ByteBuffer.allocateDirect(102400))
            }
            printCustomStackTrace()
        }

        override fun onReadCompleted(request: UrlRequest?, info: UrlResponseInfo?, byteBuffer: ByteBuffer?) {
            Log.d("HTTPClient", "Executing onReadComplete")
            byteBuffer?.flip()
            while (byteBuffer?.hasRemaining() == true) {
                buffer.write(byteBuffer.get().toInt())
            }
            byteBuffer?.clear()
            request?.read(byteBuffer)
            printCustomStackTrace()
            Log.d("HTTPClient", "EXITING onReadComplete")
        }

        override fun onSucceeded(request: UrlRequest?, info: UrlResponseInfo?) {
            Log.d("HTTPClient", "Request succeeded")
            printCustomStackTrace()
            responseBody = buffer.toString()

            if (info?.negotiatedProtocol != null) {
                protocol = info.negotiatedProtocol
            } else {
                protocol = URI.create(url).scheme
            }
            Log.d("HTTPClient", "Response Body: $responseBody")
            Log.d("HTTPClient", "Protocol: $protocol")
        }

        override fun onFailed(request: UrlRequest?, info: UrlResponseInfo?, errorInfo: CronetException?) {
            Log.e("HTTPClient", "Request failed: ${errorInfo?.toString()}")
        }

        override fun onRedirectReceived(request: UrlRequest?, info: UrlResponseInfo?, newLocationUrl: String?) {
            Log.d("HTTPClient", "Redirect received to: $newLocationUrl")
            request?.followRedirect()
        }
    }
}

