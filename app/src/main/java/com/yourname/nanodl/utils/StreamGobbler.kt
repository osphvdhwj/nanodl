package com.yourname.nanodl.utils

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class StreamGobbler(
    private val inputStream: InputStream,
    private val onProgress: (Int, String) -> Unit
) : Thread() {

    override fun run() {
        try {
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val text = line ?: continue
                    
                    // Parse yt-dlp output: "[download]  45.0% of 50.00MiB..."
                    if (text.contains("[download]") && text.contains("%")) {
                        try {
                            val percentStr = text.substringAfter("[download]").substringBefore("%").trim()
                            val progress = percentStr.toFloat().toInt()
                            
                            // Extract speed and ETA for the notification
                            val cleanText = text.replace("[download]", "").trim()
                            onProgress(progress, cleanText)
                        } catch (e: Exception) {
                            // Ignore parsing errors, just pass the raw text
                            onProgress(0, text)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
