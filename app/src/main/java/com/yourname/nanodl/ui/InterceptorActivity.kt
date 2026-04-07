package com.yourname.nanodl.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.yourname.nanodl.extractor.YouTubeBrain
import com.yourname.nanodl.service.DownloadEngineService
import com.yourname.nanodl.utils.ConfigManager
import kotlinx.coroutines.launch

class InterceptorActivity : Activity() {
    private lateinit var config: ConfigManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        config = ConfigManager(this)

        val url = intent.dataString ?: intent.getStringExtra(Intent.EXTRA_TEXT) ?: return finish()
        val cleanUrl = extractUrl(url)

        lifecycleScope.launch {
            try {
                val result = YouTubeBrain.getNativeStreams(cleanUrl, config.downloadSubtitles)
                
                if (result.videoStream != null && result.audioStream != null) {
                    val finalFileName = config.buildFileName(
                        title = result.title,
                        id = result.videoId,
                        resolution = result.resolution,
                        ext = "mp4"
                    )
                    startDownloadService(result, finalFileName)
                }
            } catch (e: Exception) {
                Toast.makeText(this@InterceptorActivity, e.message, Toast.LENGTH_LONG).show()
            } finally {
                finish()
            }
        }
    }

    private fun extractUrl(text: String): String {
        return "(?i)\\bhttps?://(www\\.)?youtu.*".toRegex().find(text)?.value ?: text
    }

    private fun startDownloadService(result: com.yourname.nanodl.extractor.ExtractionResult, fileName: String) {
        val serviceIntent = Intent(this, DownloadEngineService::class.java).apply {
            putExtra("VIDEO_URL", result.videoStream?.url)
            putExtra("AUDIO_URL", result.audioStream?.url)
            putExtra("SUB_URL", result.subtitleUrl)
            putExtra("FILE_NAME", fileName)
        }
        startForegroundService(serviceIntent)
    }
}
