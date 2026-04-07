package com.yourname.nanodl.service

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaMuxer
import java.nio.ByteBuffer

object NativeMuxer {
    fun muxNativeStreams(videoPath: String, audioPath: String, outputPath: String) {
        val muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        
        val videoExtractor = MediaExtractor().apply { setDataSource(videoPath) }
        val audioExtractor = MediaExtractor().apply { setDataSource(audioPath) }

        videoExtractor.selectTrack(0)
        audioExtractor.selectTrack(0)

        val videoTrackIndex = muxer.addTrack(videoExtractor.getTrackFormat(0))
        val audioTrackIndex = muxer.addTrack(audioExtractor.getTrackFormat(0))

        muxer.start()

        val bufferSize = 1024 * 1024 
        val buffer = ByteBuffer.allocate(bufferSize)
        val bufferInfo = MediaCodec.BufferInfo()

        while (true) {
            val sampleSize = videoExtractor.readSampleData(buffer, 0)
            if (sampleSize < 0) break
            bufferInfo.apply { offset = 0; size = sampleSize; flags = videoExtractor.sampleFlags; presentationTimeUs = videoExtractor.sampleTime }
            muxer.writeSampleData(videoTrackIndex, buffer, bufferInfo)
            videoExtractor.advance()
        }

        while (true) {
            val sampleSize = audioExtractor.readSampleData(buffer, 0)
            if (sampleSize < 0) break
            bufferInfo.apply { offset = 0; size = sampleSize; flags = audioExtractor.sampleFlags; presentationTimeUs = audioExtractor.sampleTime }
            muxer.writeSampleData(audioTrackIndex, buffer, bufferInfo)
            audioExtractor.advance()
        }

        videoExtractor.release()
        audioExtractor.release()
        muxer.stop()
        muxer.release()
    }
}
