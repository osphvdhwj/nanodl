package com.yourname.nanodl.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import com.yourname.nanodl.R
import com.yourname.nanodl.extractor.YouTubeBrain
import com.yourname.nanodl.service.DownloadEngineService
import com.yourname.nanodl.utils.ConfigManager
import kotlinx.coroutines.*

class InterceptorActivity : Activity() {
    private lateinit var config: ConfigManager
    private var extractionResult: com.yourname.nanodl.extractor.ExtractionResult? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bottom_sheet)
        config = ConfigManager(this)

        val titleView = findViewById<TextView>(R.id.sheetTitle)
        val spinner = findViewById<ProgressBar>(R.id.loadingSpinner)
        val formatGroup = findViewById<RadioGroup>(R.id.formatGroup)
        val downloadBtn = findViewById<Button>(R.id.downloadBtn)

        val url = intent.dataString ?: intent.getStringExtra(Intent.EXTRA_TEXT) ?: return finish()
        val cleanUrl = extractUrl(url)

        scope.launch {
            try {
                val result = YouTubeBrain.getNativeStreams(cleanUrl, config.downloadSubtitles)
                extractionResult = result

                titleView.text = result.title
                spinner.visibility = View.GONE
                formatGroup.visibility = View.VISIBLE
                downloadBtn.visibility = View.VISIBLE

                // Add 1080p/720p Video Option
                if (result.videoStream != null) {
                    val rbVideo = RadioButton(this@InterceptorActivity).apply {
                        text = "Video (${result.resolution})"
                        id = 1
                    }
                    formatGroup.addView(rbVideo)
                    formatGroup.check(1)
                }

                // Add Audio Only Option
                if (result.audioStream != null) {
                    val rbAudio = RadioButton(this@InterceptorActivity).apply {
                        text = "Audio Only (M4A)"
                        id = 2
                    }
                    formatGroup.addView(rbAudio)
                    if (result.videoStream == null) formatGroup.check(2)
                }

            } catch (e: Exception) {
                Toast.makeText(this@InterceptorActivity, e.message, Toast.LENGTH_LONG).show()
                finish()
            }
        }

        downloadBtn.setOnClickListener {
            val res = extractionResult ?: return@setOnClickListener
            val isAudioOnly = formatGroup.checkedRadioButtonId == 2

            val finalVideo = if (isAudioOnly) null else res.videoStream
            val resStr = if (isAudioOnly) "Audio" else res.resolution
            val extStr = if (isAudioOnly) "m4a" else "mp4"

            val finalFileName = config.buildFileName(res.title, res.videoId, resStr, extStr)

            val serviceIntent = Intent(this, DownloadEngineService::class.java).apply {
                putExtra("VIDEO_URL", finalVideo?.url)
                putExtra("AUDIO_URL", res.audioStream?.url)
                putExtra("SUB_URL", res.subtitleUrl)
                putExtra("FILE_NAME", finalFileName)
            }
            startForegroundService(serviceIntent)
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
