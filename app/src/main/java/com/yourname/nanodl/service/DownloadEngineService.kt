package com.yourname.nanodl.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
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
        // Fixed: Explicit null-check removes the compiler's safe-call warnings
        if (intent == null) return START_NOT_STICKY
        
        val videoUrl = intent.getStringExtra("VIDEO_URL")
        val audioUrl = intent.getStringExtra("AUDIO_URL")
        val subUrl = intent.getStringExtra("SUB_URL")
        val fileName = intent.getStringExtra("FILE_NAME") ?: "download.mp4"

        if (videoUrl == null && audioUrl == null) return START_NOT_STICKY

        val channelId = "NanoChannel"
        val manager = getSystemService(NotificationManager::class.java)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Active Downloads", NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }

        val notification = Notification.Builder(this, channelId)
            .setContentTitle("NanoDL Engine")
            .setContentText("Downloading $fileName...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(1, notification)
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // By default, writing to the public Movies directory
                val outputDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                if (!outputDir.exists()) outputDir.mkdirs()
                val finalVideoFile = File(outputDir, fileName)

                if (videoUrl != null && audioUrl != null) {
                    val videoCache = File(cacheDir, "temp_vid.mp4")
                    val audioCache = File(cacheDir, "temp_aud.m4a")
                    
                    downloadFile(videoUrl, videoCache)
                    downloadFile(audioUrl, audioCache)

                    NativeMuxer.muxNativeStreams(
                        videoCache.absolutePath, 
                        audioCache.absolutePath, 
                        finalVideoFile.absolutePath
                    )

                    videoCache.delete()
                    audioCache.delete()
                } else if (audioUrl != null) {
                    downloadFile(audioUrl, finalVideoFile) // Audio Only
                }

                if (subUrl != null) {
                    val subFileName = fileName.substringBeforeLast(".") + ".vtt"
                    downloadFile(subUrl, File(outputDir, subFileName))
                }

                val successNotif = Notification.Builder(this@DownloadEngineService, channelId)
                    .setContentTitle("Download Complete")
                    .setContentText("Saved: $fileName")
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .build()
                manager.notify(2, successNotif)

            } catch (e: Exception) {
                val errorNotif = Notification.Builder(this@DownloadEngineService, channelId)
                    .setContentTitle("Download Failed")
                    .setContentText(e.message)
                    .setSmallIcon(android.R.drawable.stat_notify_error)
                    .build()
                manager.notify(3, errorNotif)
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
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
