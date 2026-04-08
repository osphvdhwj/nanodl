package com.yourname.nanodl.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.yourname.nanodl.R
import com.yourname.nanodl.extractor.YouTubeBrain
import com.yourname.nanodl.utils.ConfigManager
import com.yourname.nanodl.work.DownloadWorker
import kotlinx.coroutines.*

class InterceptorActivity : Activity() {
    private lateinit var config: ConfigManager
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var selectedFormatId = "best"
    private var originalUrl = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bottom_sheet_ytdlnis)
        config = ConfigManager(this)

        val titleView = findViewById<TextView>(R.id.videoTitle)
        val spinner = findViewById<ProgressBar>(R.id.loadingSpinner)
        val formatContainer = findViewById<LinearLayout>(R.id.formatContainer)
        val downloadBtn = findViewById<Button>(R.id.btnDownload)

        val url = intent.dataString ?: intent.getStringExtra(Intent.EXTRA_TEXT) ?: return finish()
        originalUrl = extractUrl(url)

        // Simulate extraction and YTDLnis format population
        scope.launch {
            try {
                val result = YouTubeBrain.getNativeStreams(originalUrl, config.downloadSubtitles)
                titleView.text = result.title
                spinner.visibility = View.GONE
                formatContainer.visibility = View.VISIBLE
                
                // We default to "best" for now since we are hooking into yt-dlp
                downloadBtn.isEnabled = true
                downloadBtn.alpha = 1.0f

            } catch (e: Exception) {
                Toast.makeText(this@InterceptorActivity, "Extraction failed.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        downloadBtn.setOnClickListener {
            val fileName = config.buildFileName(titleView.text.toString(), "video", "best", "mp4")

            // --- THE FIX: Pass the job to WorkManager ---
            val inputData = Data.Builder()
                .putString("URL", originalUrl)
                .putString("FORMAT_ID", selectedFormatId)
                .putString("FILE_NAME", fileName)
                .build()

            val downloadWorkRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(applicationContext).enqueue(downloadWorkRequest)
            
            Toast.makeText(this, "Download started...", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun extractUrl(text: String): String {
        return "(?i)\\bhttps?://(www\\.)?youtu.*".toRegex().find(text)?.value ?: text
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
