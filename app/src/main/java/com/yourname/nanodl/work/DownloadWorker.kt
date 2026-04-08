package com.yourname.nanodl.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result as WorkResult
import com.yourname.nanodl.utils.RootHelper
import com.yourname.nanodl.utils.StreamGobbler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class DownloadWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): WorkResult = withContext(Dispatchers.IO) {
        val url = inputData.getString("URL") ?: return@withContext WorkResult.failure()
        val formatId = inputData.getString("FORMAT_ID") ?: "best"
        val fileName = inputData.getString("FILE_NAME") ?: "download.mp4"
        
        createChannel()
        setForeground(createForegroundInfo("Starting engine...", 0))

        val outputDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val outputPath = File(outputDir, fileName).absolutePath

        val commandList = mutableListOf(
            "yt-dlp",
            "-f", formatId,
            "-o", outputPath,
            "--no-mtime"
        )

        // Inject Root Cookies if available
        if (RootHelper.hasRootAccess()) {
            val cookieDbPath = RootHelper.extractChromeCookies(applicationContext.cacheDir)
            if (cookieDbPath != null) {
                commandList.add("--cookies")
                commandList.add(cookieDbPath)
            }
        }

        commandList.add(url)

        try {
            val processBuilder = ProcessBuilder(commandList)
            processBuilder.redirectErrorStream(true)
            val process = processBuilder.start()

            val gobbler = StreamGobbler(process.inputStream) { progress, text ->
                val info = createForegroundInfo(text, progress)
                // Launch in GlobalScope so it doesn't suspend the gobbler thread
                GlobalScope.launch {
                    try { setForeground(info) } catch (e: Exception) {}
                }
            }
            gobbler.start()

            val exitCode = process.waitFor()
            gobbler.join()

            if (exitCode == 0) {
                val successNotif = NotificationCompat.Builder(applicationContext, "DOWNLOADS")
                    .setContentTitle("Download Complete")
                    .setContentText(fileName)
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .build()
                notificationManager.notify(url.hashCode(), successNotif)
                
                WorkResult.success()
            } else {
                WorkResult.failure()
            }

        } catch (e: Exception) {
            WorkResult.failure()
        }
    }

    private fun createForegroundInfo(progressText: String, progress: Int): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, "DOWNLOADS")
            .setContentTitle("NanoDL is Downloading")
            .setContentText(progressText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .build()
            
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
