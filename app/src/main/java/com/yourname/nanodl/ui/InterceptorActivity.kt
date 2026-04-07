package com.yourname.nanodl.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import com.yourname.nanodl.R
import com.yourname.nanodl.extractor.YouTubeBrain
import com.yourname.nanodl.service.DownloadEngineService
import com.yourname.nanodl.utils.ConfigManager
import org.schabi.newpipe.extractor.stream.VideoStream
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
        val selectQualityLabel = findViewById<TextView>(R.id.selectQualityLabel)

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
                selectQualityLabel.visibility = View.VISIBLE

                var firstId = -1

                // Dynamically build the Radio Buttons based on available formats
                result.videoStreams.forEachIndexed { index, stream ->
                    val rbVideo = RadioButton(this@InterceptorActivity).apply {
                        text = "Video (${stream.getResolution()})"
                        id = index + 1
                    }
                    formatGroup.addView(rbVideo)
                    if (firstId == -1) firstId = rbVideo.id
                }

                if (result.bestAudio != null) {
                    val audioId = result.videoStreams.size + 1
                    val rbAudio = RadioButton(this@InterceptorActivity).apply {
                        text = "Audio Only (M4A)"
                        id = audioId
                    }
                    formatGroup.addView(rbAudio)
                    if (firstId == -1) firstId = audioId
                }

                if (firstId != -1) formatGroup.check(firstId)

            } catch (e: Exception) {
                Log.e("NanoDL_Crash", "Extraction Engine Failed", e)
                Toast.makeText(this@InterceptorActivity, e.message, Toast.LENGTH_LONG).show()
                finish()
            }
        }

        downloadBtn.setOnClickListener {
            val res = extractionResult ?: return@setOnClickListener
            val selectedId = formatGroup.checkedRadioButtonId
            
            val isAudioOnly = selectedId == res.videoStreams.size + 1

            val finalVideo: VideoStream? = if (isAudioOnly) null else res.videoStreams.getOrNull(selectedId - 1)
            val resStr = if (isAudioOnly) "Audio" else finalVideo?.getResolution() ?: "Unknown"
            val extStr = if (isAudioOnly) "m4a" else "mp4"

            val finalFileName = config.buildFileName(res.title, res.videoId, resStr, extStr)

            val serviceIntent = Intent(this, DownloadEngineService::class.java).apply {
                putExtra("VIDEO_URL", finalVideo?.content) // Fixed: .url is deprecated
                putExtra("AUDIO_URL", res.bestAudio?.content)
                putExtra("SUB_URL", res.subtitleUrl)
                putExtra("FILE_NAME", finalFileName)
            }
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
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
