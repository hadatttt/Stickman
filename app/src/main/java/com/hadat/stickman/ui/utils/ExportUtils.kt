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
import com.hadat.stickman.utils.AnimatedGifEncoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.net.URL

object ExportUtils {

    /**
     * Xuất danh sách Bitmap thành file GIF sử dụng AnimatedGifEncoder.
     * @param context Context của ứng dụng
     * @param frames Danh sách các Bitmap làm frame của GIF
     * @param frameRate Số frame mỗi giây
     * @param projectName Tên dự án để tạo tên file
     * @param backgroundUrl URL hình nền (có thể null hoặc rỗng)
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
                if (frames.isEmpty()) throw IllegalArgumentException("Frame list is empty")
                if (frameRate <= 0) throw IllegalArgumentException("Invalid frame rate")

                val width = frames[0].width
                val height = frames[0].height

                val backgroundBitmap: Bitmap = if (!backgroundUrl.isNullOrEmpty()) {
                    try {
                        val url = URL(backgroundUrl)
                        url.openStream().use { inputStream ->
                            BitmapFactory.decodeStream(inputStream)
                        } ?: Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                            eraseColor(Color.WHITE)
                        }
                    } catch (e: Exception) {
                        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                            eraseColor(Color.WHITE)
                        }
                    }
                } else {
                    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                        eraseColor(Color.WHITE)
                    }
                }

                val scaledBackground = Bitmap.createScaledBitmap(backgroundBitmap, width, height, true)

                val fileName = "$projectName-${System.currentTimeMillis()}.gif"
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/gif")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/StickmanExports")
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: throw Exception("Failed to create MediaStore record")

                resolver.openOutputStream(uri)?.use { outputStream ->
                    val encoder = AnimatedGifEncoder()
                    encoder.start(outputStream)
                    encoder.setDelay(1000 / frameRate)
                    encoder.setRepeat(0)

                    frames.forEachIndexed { index, frame ->
                        val combined = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(combined)
                        canvas.drawBitmap(scaledBackground, 0f, 0f, null)

                        val scaledFrame = if (frame.width != width || frame.height != height) {
                            Bitmap.createScaledBitmap(frame, width, height, true)
                        } else frame

                        canvas.drawBitmap(scaledFrame, 0f, 0f, null)
                        encoder.addFrame(combined)

                        if (scaledFrame != frame) scaledFrame.recycle()
                        combined.recycle()

                        android.util.Log.d("ExportUtils", "Added GIF frame $index")
                    }

                    encoder.finish()
                } ?: throw Exception("Failed to open output stream")

                if (!backgroundBitmap.isRecycled) backgroundBitmap.recycle()
                if (!scaledBackground.isRecycled) scaledBackground.recycle()

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
     * Xuất danh sách Bitmap thành file MP4 sử dụng MediaCodec.
     * @param context Context của ứng dụng
     * @param frames Danh sách các Bitmap làm frame của video
     * @param frameRate Số frame mỗi giây
     * @param projectName Tên dự án để tạo tên file
     * @param backgroundUrl URL hình nền (có thể null hoặc rỗng)
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
                if (frames.isEmpty()) throw IllegalArgumentException("Frame list is empty")
                if (frameRate <= 0) throw IllegalArgumentException("Invalid frame rate")

                val width = frames[0].width
                val height = frames[0].height

                val backgroundBitmap: Bitmap = if (!backgroundUrl.isNullOrEmpty()) {
                    try {
                        val url = URL(backgroundUrl)
                        url.openStream().use { inputStream ->
                            BitmapFactory.decodeStream(inputStream)
                        } ?: Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                            eraseColor(Color.WHITE)
                        }
                    } catch (e: Exception) {
                        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                            eraseColor(Color.WHITE)
                        }
                    }
                } else {
                    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                        eraseColor(Color.WHITE)
                    }
                }

                val scaledBackground = Bitmap.createScaledBitmap(backgroundBitmap, width, height, true)

                val fileName = "$projectName-${System.currentTimeMillis()}.mp4"
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
                    setInteger(MediaFormat.KEY_BIT_RATE, 2_000_000)
                    setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                    setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                }

                val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
                codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                val inputSurface: Surface = codec.createInputSurface()
                if (!inputSurface.isValid) {
                    throw Exception("Input surface is not valid")
                }
                codec.start()

                resolver.openOutputStream(uri)?.use { outputStream ->
                    val tempFile = File(context.cacheDir, fileName)
                    val muxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                    val videoTrackIndex = muxer.addTrack(codec.outputFormat)
                    muxer.start()

                    val bufferInfo = MediaCodec.BufferInfo()
                    val frameIntervalNanos = (1_000_000_000L / frameRate)

                    frames.forEachIndexed { index, frame ->
                        if (!inputSurface.isValid) throw Exception("Surface is not valid at frame $index")

                        val canvas = inputSurface.lockCanvas(null)
                        canvas.drawBitmap(scaledBackground, 0f, 0f, null)

                        val scaledFrame = if (frame.width != width || frame.height != height) {
                            Bitmap.createScaledBitmap(frame, width, height, true)
                        } else frame

                        canvas.drawBitmap(scaledFrame, 0f, 0f, null)
                        inputSurface.unlockCanvasAndPost(canvas)
                        if (scaledFrame != frame) scaledFrame.recycle()

                        var outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 100_000)
                        while (outputBufferIndex >= 0) {
                            val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                            if (outputBuffer != null && bufferInfo.size > 0) {
                                bufferInfo.presentationTimeUs = index * frameIntervalNanos / 1000
                                muxer.writeSampleData(videoTrackIndex, outputBuffer, bufferInfo)
                            }
                            codec.releaseOutputBuffer(outputBufferIndex, false)
                            outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 100_000)
                        }
                        android.util.Log.d("ExportUtils", "Added MP4 frame $index")
                    }

                    // Xử lý buffer cuối
                    var outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 100_000)
                    while (outputBufferIndex >= 0) {
                        val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                        if (outputBuffer != null && bufferInfo.size > 0) {
                            bufferInfo.presentationTimeUs = frames.size * frameIntervalNanos / 1000
                            muxer.writeSampleData(videoTrackIndex, outputBuffer, bufferInfo)
                        }
                        codec.releaseOutputBuffer(outputBufferIndex, false)
                        outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 100_000)
                    }

                    codec.stop()
                    codec.release()
                    muxer.stop()
                    muxer.release()

                    tempFile.inputStream().use { input ->
                        outputStream.write(input.readBytes())
                    }
                    tempFile.delete()
                } ?: throw Exception("Failed to open output stream")

                if (!backgroundBitmap.isRecycled) backgroundBitmap.recycle()
                if (!scaledBackground.isRecycled) scaledBackground.recycle()

                if (inputSurface.isValid) inputSurface.release()

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
}
