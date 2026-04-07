package com.yourname.nanodl.extractor

import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamExtractor
import org.schabi.newpipe.extractor.stream.VideoStream
import org.schabi.newpipe.extractor.stream.AudioStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ExtractionResult(
    val videoId: String,
    val title: String,
    val videoStream: VideoStream?,
    val audioStream: AudioStream?,
    val subtitleUrl: String?,
    val resolution: String
)

object YouTubeBrain {
    suspend fun getNativeStreams(youtubeUrl: String, fetchSubtitles: Boolean): ExtractionResult {
        return withContext(Dispatchers.IO) {
            val extractor: StreamExtractor = ServiceList.YouTube.getStreamExtractor(youtubeUrl)
            extractor.fetchPage()

            // Fixed: Added safe calls (?.) to format and resolution
            val bestVideo = extractor.videoOnlyStreams
                .filter { it.format?.name == "MPEG_4" }
                .maxByOrNull { it.resolution?.replace("p", "")?.toIntOrNull() ?: 0 }

            // Fixed: Added safe calls (?.) to format
            val bestAudio = extractor.audioStreams
                .filter { it.format?.name == "M4A" }
                .maxByOrNull { it.averageBitrate }

            var subUrl: String? = null
            if (fetchSubtitles && !extractor.subtitlesDefault.isNullOrEmpty()) {
                val bestSub = extractor.subtitlesDefault.find { it.languageTag.contains("en") } 
                    ?: extractor.subtitlesDefault.first()
                subUrl = bestSub.url
            }

            ExtractionResult(
                videoId = extractor.id ?: "unknown",
                title = extractor.name ?: "Unknown Title",
                videoStream = bestVideo,
                audioStream = bestAudio,
                subtitleUrl = subUrl,
                resolution = bestVideo?.resolution ?: "AudioOnly"
            )
        }
    }
}
