package com.yourname.nanodl.extractor

import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

class NativeDownloader : Downloader() {
    companion object {
        private var instance: NativeDownloader? = null
        fun getInstance(): NativeDownloader {
            if (instance == null) instance = NativeDownloader()
            return instance!!
        }
    }

    override fun execute(request: Request): Response {
        val url = URL(request.url())
        val connection = url.openConnection() as HttpURLConnection
        
        connection.requestMethod = request.httpMethod()
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.instanceFollowRedirects = false

        // 1. Filter out Brotli ('br') compression which we cannot handle natively
        request.headers().forEach { (key, list) ->
            if (key.equals("Accept-Encoding", ignoreCase = true)) {
                connection.addRequestProperty("Accept-Encoding", "gzip, deflate")
            } else {
                for (value in list) {
                    connection.addRequestProperty(key, value)
                }
            }
        }

        val responseCode = connection.responseCode
        val responseMessage = connection.responseMessage
        
        // 2. Grab the correct stream
        var stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        
        // 3. Decompress the binary data if YouTube zipped it
        val encoding = connection.contentEncoding
        if (stream != null && encoding != null) {
            if (encoding.equals("gzip", ignoreCase = true)) {
                stream = GZIPInputStream(stream)
            } else if (encoding.equals("deflate", ignoreCase = true)) {
                stream = InflaterInputStream(stream, Inflater(true))
            }
        }

        // Now we can safely read the unzipped JSON as plaintext
        val responseBody = stream?.bufferedReader()?.use { it.readText() } ?: ""

        val responseHeaders = mutableMapOf<String, List<String>>()
        connection.headerFields.forEach { (key, value) ->
            if (key != null) responseHeaders[key] = value
        }

        return Response(responseCode, responseMessage, responseHeaders, responseBody, request.url())
    }
}
