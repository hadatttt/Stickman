package com.hadat.stickman.ui.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Environment
import android.provider.MediaStore
import android.view.Surface
import android.widget.Toast
import com.hadat.stickman.utils.ImprovedAnimatedGifEncoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.net.URL

object ExportUtils {

    /**
     * Exports a list of Bitmaps to a GIF file using ImprovedAnimatedGifEncoder.
     */
    fun exportToGif(
        context: Context,
        frames: List<Bitmap>,
        frameRate: Int,
        projectName: String,
        backgroundUrl: String? = null
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                validateInput(frames, frameRate)

                val (width, height) = frames[0].let { it.width to it.height }
                val backgroundBitmap = loadBackgroundBitmap(context, backgroundUrl, width, height)
                val scaledBackground = Bitmap.createScaledBitmap(backgroundBitmap, width, height, true)

                val fileName = "${projectName}-${System.currentTimeMillis()}.gif"
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/gif")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/StickmanExports")
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: throw Exception("Failed to create MediaStore record")

                resolver.openOutputStream(uri)?.use { outputStream ->
                    val encoder = ImprovedAnimatedGifEncoder().apply {
                        setQuality(10)
                        setSize(width, height)
                        setFrameRate(frameRate.toFloat())
                        setRepeat(0)
                        start(outputStream)
                    }

                    frames.forEachIndexed { index, frame ->
                        val combined = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        Canvas(combined).apply {
                            drawBitmap(scaledBackground, 0f, 0f, null)
                            val scaledFrame = if (frame.width != width || frame.height != height) {
                                Bitmap.createScaledBitmap(frame, width, height, true)
                            } else frame
                            drawBitmap(scaledFrame, 0f, 0f, null)
                            if (scaledFrame != frame) scaledFrame.recycle()
                        }
                        encoder.addFrame(combined)
                        combined.recycle()
                        android.util.Log.d("ExportUtils", "Processed GIF frame $index")
                    }

                    encoder.finish()
                } ?: throw Exception("Failed to open output stream")

                backgroundBitmap.safeRecycle()
                scaledBackground.safeRecycle()

                launch(Dispatchers.Main) {
                    Toast.makeText(context, "GIF exported successfully to $fileName", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "GIF export failed: ${e.message}", Toast.LENGTH_LONG).show()
                    android.util.Log.e("ExportUtils", "GIF export error", e)
                }
            }
        }
    }

    /**
     * Exports a list of Bitmaps to an MP4 video using MediaCodec.
     */
    fun exportToMp4(
        context: Context,
        frames: List<Bitmap>,
        frameRate: Int,
        projectName: String,
        backgroundUrl: String? = null
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                validateInput(frames, frameRate)

                val (width, height) = frames[0].let { it.width to it.height }
                val backgroundBitmap = loadBackgroundBitmap(context, backgroundUrl, width, height)
                val scaledBackground = Bitmap.createScaledBitmap(backgroundBitmap, width, height, true)

                val fileName = "${projectName}-${System.currentTimeMillis()}.mp4"
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/StickmanExports")
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: throw Exception("Failed to create MediaStore record")

                val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                    setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                    setInteger(MediaFormat.KEY_BIT_RATE, calculateBitRate(width, height))
                    setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                    setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                }

                val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
                codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                val inputSurface = codec.createInputSurface()
                codec.start()

                resolver.openOutputStream(uri)?.use { outputStream ->
                    val tempFile = File(context.cacheDir, fileName)
                    val muxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                    val videoTrackIndex = muxer.addTrack(codec.outputFormat)
                    muxer.start()

                    val bufferInfo = MediaCodec.BufferInfo()
                    val frameIntervalNanos = 1_000_000_000L / frameRate

                    frames.forEachIndexed { index, frame ->
                        val canvas = inputSurface.lockCanvas(null)
                        canvas.drawBitmap(scaledBackground, 0f, 0f, null)
                        val scaledFrame = if (frame.width != width || frame.height != height) {
                            Bitmap.createScaledBitmap(frame, width, height, true)
                        } else frame
                        canvas.drawBitmap(scaledFrame, 0f, 0f, null)
                        inputSurface.unlockCanvasAndPost(canvas)
                        if (scaledFrame != frame) scaledFrame.recycle()

                        drainEncoder(codec, bufferInfo, muxer, videoTrackIndex, index, frameIntervalNanos)
                        android.util.Log.d("ExportUtils", "Processed MP4 frame $index")
                    }

                    drainEncoder(codec, bufferInfo, muxer, videoTrackIndex, frames.size, frameIntervalNanos)
                    codec.stop()
                    codec.release()
                    muxer.stop()
                    muxer.release()

                    tempFile.inputStream().use { input ->
                        outputStream.write(input.readBytes())
                    }
                    tempFile.delete()
                } ?: throw Exception("Failed to open output stream")

                backgroundBitmap.safeRecycle()
                scaledBackground.safeRecycle()
                inputSurface.release()

                launch(Dispatchers.Main) {
                    Toast.makeText(context, "MP4 exported successfully to $fileName", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "MP4 export failed: ${e.message}", Toast.LENGTH_LONG).show()
                    android.util.Log.e("ExportUtils", "MP4 export error", e)
                }
            }
        }
    }

    private fun validateInput(frames: List<Bitmap>, frameRate: Int) {
        if (frames.isEmpty()) throw IllegalArgumentException("Frame list is empty")
        if (frameRate <= 0) throw IllegalArgumentException("Invalid frame rate")
        if (frameRate > 60) throw IllegalArgumentException("Frame rate too high, maximum is 60")
    }

    private fun loadBackgroundBitmap(context: Context, backgroundUrl: String?, width: Int, height: Int): Bitmap {
        return if (!backgroundUrl.isNullOrEmpty()) {
            try {
                URL(backgroundUrl).openStream().use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                        ?: createDefaultBackground(width, height)
                }
            } catch (e: Exception) {
                android.util.Log.w("ExportUtils", "Failed to load background from URL", e)
                createDefaultBackground(width, height)
            }
        } else {
            createDefaultBackground(width, height)
        }
    }

    private fun createDefaultBackground(width: Int, height: Int): Bitmap {
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.WHITE)
        }
    }

    private fun calculateBitRate(width: Int, height: Int): Int {
        val pixels = width * height
        return when {
            pixels > 1920 * 1080 -> 8_000_000
            pixels > 1280 * 720 -> 4_000_000
            else -> 2_000_000
        }
    }

    private fun drainEncoder(
        codec: MediaCodec,
        bufferInfo: MediaCodec.BufferInfo,
        muxer: MediaMuxer,
        videoTrackIndex: Int,
        frameIndex: Int,
        frameIntervalNanos: Long
    ) {
        var outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 100_000)
        while (outputBufferIndex >= 0) {
            val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
            if (outputBuffer != null && bufferInfo.size > 0) {
                bufferInfo.presentationTimeUs = frameIndex * frameIntervalNanos / 1000
                muxer.writeSampleData(videoTrackIndex, outputBuffer, bufferInfo)
            }
            codec.releaseOutputBuffer(outputBufferIndex, false)
            outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 100_000)
        }
    }

    private fun Bitmap.safeRecycle() {
        if (!isRecycled) recycle()
    }
}