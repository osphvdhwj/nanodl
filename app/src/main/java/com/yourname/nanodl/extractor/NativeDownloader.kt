package com.yourname.nanodl.extractor

import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.net.HttpURLConnection
import java.net.URL

class NativeDownloader : Downloader {
    
    companion object {
        private var instance: NativeDownloader? = null
        fun getInstance(): NativeDownloader {
            if (instance == null) {
                instance = NativeDownloader()
            }
            return instance!!
        }
    }

    override fun execute(request: Request): Response {
        val url = URL(request.url())
        val connection = url.openConnection() as HttpURLConnection
        
        connection.requestMethod = request.httpMethod()
        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        // Inject headers required by NewPipe (like User-Agent)
        request.headers().forEach { (key, list) ->
            for (value in list) {
                connection.addRequestProperty(key, value)
            }
        }

        val responseCode = connection.responseCode
        val responseMessage = connection.responseMessage
        
        // Read the response body
        val responseBody = if (responseCode in 200..299) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
        }

        // Map response headers
        val responseHeaders = mutableMapOf<String, List<String>>()
        connection.headerFields.forEach { (key, value) ->
            if (key != null) {
                responseHeaders[key] = value
            }
        }

        return Response(
            responseCode,
            responseMessage,
            responseHeaders,
            responseBody,
            request.url()
        )
    }
}