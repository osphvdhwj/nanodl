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

        // --- THE BYPASS ---
        // Force the YouTube Consent Cookie to bypass the bot/GDPR block page
        connection.addRequestProperty("Cookie", "CONSENT=YES+cb.20230509-09-p0.en+FX+083")
        connection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36")

        // Add NewPipe's requested headers, but don't overwrite our bypasses
        request.headers().forEach { (key, list) ->
            if (key.equals("Accept-Encoding", ignoreCase = true)) {
                connection.addRequestProperty("Accept-Encoding", "gzip, deflate")
            } else if (!key.equals("Cookie", ignoreCase = true) && !key.equals("User-Agent", ignoreCase = true)) {
                for (value in list) {
                    connection.addRequestProperty(key, value)
                }
            }
        }

        val responseCode = connection.responseCode
        val responseMessage = connection.responseMessage
        
        var stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        
        val encoding = connection.contentEncoding
        if (stream != null && encoding != null) {
            if (encoding.equals("gzip", ignoreCase = true)) {
                stream = GZIPInputStream(stream)
            } else if (encoding.equals("deflate", ignoreCase = true)) {
                stream = InflaterInputStream(stream, Inflater(true))
            }
        }

        val responseBody = stream?.bufferedReader()?.use { it.readText() } ?: ""

        val responseHeaders = mutableMapOf<String, List<String>>()
        connection.headerFields.forEach { (key, value) ->
            if (key != null) responseHeaders[key] = value
        }

        return Response(responseCode, responseMessage, responseHeaders, responseBody, request.url())
    }
}
