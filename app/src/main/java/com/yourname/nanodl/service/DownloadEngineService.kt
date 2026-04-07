package com.yourname.nanodl.service

import android.app.Service
import android.content.Intent
import android.os.Environment
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class DownloadEngineService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val videoUrl = intent?.getStringExtra("VIDEO_URL") ?: return START_NOT_STICKY
        val audioUrl = intent?.getStringExtra("AUDIO_URL") ?: return START_NOT_STICKY
        val subUrl = intent?.getStringExtra("SUB_URL")
        val fileName = intent?.getStringExtra("FILE_NAME") ?: "download.mp4"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val videoCache = File(cacheDir, "temp_vid.mp4")
                val audioCache = File(cacheDir, "temp_aud.m4a")
                
                downloadFile(videoUrl, videoCache)
                downloadFile(audioUrl, audioCache)

                val outputDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                val finalVideoFile = File(outputDir, fileName)

                NativeMuxer.muxNativeStreams(
                    videoCache.absolutePath, 
                    audioCache.absolutePath, 
                    finalVideoFile.absolutePath
                )

                if (subUrl != null) {
                    val subFileName = fileName.replace(".mp4", ".vtt")
                    downloadFile(subUrl, File(outputDir, subFileName))
                }

                videoCache.delete()
                audioCache.delete()
            } catch (e: Exception) {
                // Handle failure
            } finally {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun downloadFile(urlStr: String, destFile: File) {
        val connection = URL(urlStr).openConnection() as HttpURLConnection
        connection.connect()
        connection.inputStream.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
