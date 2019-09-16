package com.example.androidmediapractice.main.task4

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaMuxer
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

class MediaMuxHelper(inputPath: String, outputPath: String) {
    private val mediaExtractor = MediaExtractor().apply { setDataSource(inputPath) }
    private val mediaMuxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

    fun mux(context: Context) = GlobalScope.launch(Dispatchers.IO) {
        for (i in 0 until mediaExtractor.trackCount) {
            val trackFormat = mediaExtractor.getTrackFormat(i)
            mediaMuxer.addTrack(trackFormat)
        }
        mediaMuxer.start()
        for (i in 0 until mediaExtractor.trackCount) {
            writeToMuxer(i)
        }
        mediaMuxer.stop()
        GlobalScope.launch(Dispatchers.Main) {
            Toast.makeText(context, "Mux Success", Toast.LENGTH_SHORT).show()
        }
    }

    fun release() {
        mediaExtractor.release()
        mediaMuxer.release()
    }

    private fun writeToMuxer(trackIndex: Int) {
        mediaExtractor.selectTrack(trackIndex)
        do {
            val buffer = ByteBuffer.wrap(ByteArray(1024 * 1024))
            val bufferInfo = MediaCodec.BufferInfo()
            val size = mediaExtractor.readSampleData(buffer, 0)
            if (size < 0) {
                break
            }
            bufferInfo.flags = mediaExtractor.sampleFlags
            bufferInfo.offset = 0
            bufferInfo.size = size
            bufferInfo.presentationTimeUs += mediaExtractor.sampleTime
            mediaMuxer.writeSampleData(trackIndex, buffer, bufferInfo)
            val isEos =
                mediaExtractor.sampleFlags and MediaCodec.BUFFER_FLAG_END_OF_STREAM == MediaCodec.BUFFER_FLAG_END_OF_STREAM
            mediaExtractor.advance()
            buffer.clear()
        } while (!isEos)
    }
}