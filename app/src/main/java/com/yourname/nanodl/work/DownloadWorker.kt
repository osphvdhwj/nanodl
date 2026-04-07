package com.yourname.nanodl.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result as WorkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DownloadWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): WorkResult = withContext(Dispatchers.IO) {
        val url = inputData.getString("URL") ?: return@withContext WorkResult.failure()
        val formatId = inputData.getString("FORMAT_ID") ?: "best"
        
        // Android 14 WorkManager Foreground rules
        createChannel()
        setForeground(createForegroundInfo(url, 0))

        try {
            // Simulate Download for UI testing
            for (i in 1..100 step 10) {
                setForeground(createForegroundInfo("Downloading...", i))
                kotlinx.coroutines.delay(500)
            }

            WorkResult.success()
        } catch (e: Exception) {
            WorkResult.failure()
        }
    }

    private fun createForegroundInfo(progressText: String, progress: Int): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, "DOWNLOADS")
            .setContentTitle("NanoDL Download")
            .setContentText(progressText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .build()
            
        // Explicitly declare Data Sync type for Android 14 restrictions
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(1, notification)
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("DOWNLOADS", "Active Downloads", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
