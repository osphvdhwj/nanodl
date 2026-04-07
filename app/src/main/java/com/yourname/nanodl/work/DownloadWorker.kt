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

        // 1. Build the exact yt-dlp arguments like YTDLnis does
        val commandList = mutableListOf(
            "yt-dlp",
            "-f", formatId,
            "-o", outputPath,
            "--no-mtime" // Don't copy the original upload date, use current time
        )

        // 2. The Killer Feature: Inject Root Cookies if available to bypass age/member restrictions
        if (RootHelper.hasRootAccess()) {
            val cookieDbPath = RootHelper.extractChromeCookies(applicationContext.cacheDir)
            if (cookieDbPath != null) {
                commandList.add("--cookies")
                commandList.add(cookieDbPath)
            }
        }

        commandList.add(url)

        try {
            // 3. Execute the binary in a sandboxed process
            val processBuilder = ProcessBuilder(commandList)
            
            // Note: In a full production build, you would set the python PATH here 
            // e.g., processBuilder.environment()["PATH"] = pythonDir.absolutePath
            processBuilder.redirectErrorStream(true) // Merge errors into standard output
            
            val process = processBuilder.start()

            // 4. Gobble the terminal output and push it to Android Notifications
            val gobbler = StreamGobbler(process.inputStream) { progress, text ->
                // Update WorkManager Foreground Notification
                val info = createForegroundInfo(text, progress)
                // Use a standard coroutine launch to avoid blocking the gobbler thread
                kotlinx.coroutines.GlobalScope.launch {
                    try { setForeground(info) } catch (e: Exception) {}
                }
            }
            gobbler.start()

            // Wait for yt-dlp to finish downloading and muxing
            val exitCode = process.waitFor()
            gobbler.join()

            if (exitCode == 0) {
                // Success! Show final completion notification
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
            .setProgress(100, progress, progress == 0) // Indeterminate if 0
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
