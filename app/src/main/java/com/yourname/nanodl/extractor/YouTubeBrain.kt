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
    val videoStreams: List<VideoStream>,
    val bestAudio: AudioStream?,
    val subtitleUrl: String?
)

object YouTubeBrain {
    suspend fun getNativeStreams(youtubeUrl: String, fetchSubtitles: Boolean): ExtractionResult {
        return withContext(Dispatchers.IO) {
            val extractor: StreamExtractor = ServiceList.YouTube.getStreamExtractor(youtubeUrl)
            extractor.fetchPage()

            // Sort native MP4 streams by resolution to populate the UI choices
            val videoStreams = extractor.videoOnlyStreams
                .filter { it.format?.name == "MPEG_4" }
                .sortedByDescending { it.getResolution()?.replace("p", "")?.toIntOrNull() ?: 0 }
                .distinctBy { it.getResolution() }

            val bestAudio = extractor.audioStreams
                .filter { it.format?.name == "M4A" }
                .maxByOrNull { it.averageBitrate }

            var subUrl: String? = null
            if (fetchSubtitles && !extractor.subtitlesDefault.isNullOrEmpty()) {
                val bestSub = extractor.subtitlesDefault.find { it.languageTag.contains("en") } 
                    ?: extractor.subtitlesDefault.first()
                subUrl = bestSub.content // Fixed: .url is deprecated
            }

            ExtractionResult(
                videoId = extractor.id, // Fixed: Elvis ?: operator removed for non-null types
                title = extractor.name,
                videoStreams = videoStreams,
                bestAudio = bestAudio,
                subtitleUrl = subUrl
            )
        }
    }
}
