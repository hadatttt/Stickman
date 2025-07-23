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
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.hadat.stickman.utils.AnimatedGifEncoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.nio.ByteBuffer

object ExportUtils {

    /**
     * Xuất danh sách Bitmap thành file GIF sử dụng AnimatedGifEncoder.
     * @param context Context của ứng dụng
     * @param frames Danh sách các Bitmap làm frame của GIF
     * @param frameRate Số frame mỗi giây
     * @param projectName Tên dự án để tạo tên file
     * @param idBackground Resource ID của hình nền (hình hoặc màu)
     */
    fun exportToGif(
        context: Context,
        frames: List<Bitmap>,
        frameRate: Int,
        projectName: String,
        @DrawableRes idBackground: Int
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (frames.isEmpty()) throw IllegalArgumentException("Frame list is empty")
                if (frameRate <= 0) throw IllegalArgumentException("Invalid frame rate")

                // Lấy kích thước từ frame đầu tiên
                val width = frames[0].width
                val height = frames[0].height

                // Tạo nền
                val backgroundBitmap = BitmapFactory.decodeResource(context.resources, idBackground)
                    ?: Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                        eraseColor(ContextCompat.getColor(context, idBackground))
                    }
                val scaledBackground = Bitmap.createScaledBitmap(backgroundBitmap, width, height, true)

                // Tạo tên file GIF với timestamp
                val fileName = "$projectName-${System.currentTimeMillis()}.gif"

                // Thiết lập ContentValues cho MediaStore
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/gif")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/StickmanExports")
                }

                // Tạo file trong MediaStore
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: throw Exception("Failed to create MediaStore record")

                // Ghi GIF
                resolver.openOutputStream(uri)?.use { outputStream ->
                    val encoder = AnimatedGifEncoder()
                    encoder.start(outputStream)
                    encoder.setDelay(1000 / frameRate) // Thời gian mỗi frame (ms)
                    encoder.setRepeat(0) // Lặp vô hạn

                    frames.forEachIndexed { index, frame ->
                        val combined = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(combined)
                        canvas.drawBitmap(scaledBackground, 0f, 0f, null)
                        val scaledFrame = if (frame.width != width || frame.height != height) {
                            Bitmap.createScaledBitmap(frame, width, height, true)
                        } else {
                            frame
                        }
                        canvas.drawBitmap(scaledFrame, 0f, 0f, null)
                        encoder.addFrame(combined)
                        if (scaledFrame != frame) scaledFrame.recycle()
                        combined.recycle()
                        android.util.Log.d("ExportUtils", "Added GIF frame $index")
                    }

                    encoder.finish()
                } ?: throw Exception("Failed to open output stream")

                // Giải phóng backgroundBitmap
                if (!backgroundBitmap.isRecycled) backgroundBitmap.recycle()
                if (!scaledBackground.isRecycled) scaledBackground.recycle()

                // Thông báo thành công
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
     * @param idBackground Resource ID của hình nền (hình hoặc màu)
     */
    fun exportToMp4(
        context: Context,
        frames: List<Bitmap>,
        frameRate: Int,
        projectName: String,
        @DrawableRes idBackground: Int
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (frames.isEmpty()) throw IllegalArgumentException("Frame list is empty")
                if (frameRate <= 0) throw IllegalArgumentException("Invalid frame rate")

                // Lấy kích thước từ frame đầu tiên
                val width = frames[0].width
                val height = frames[0].height

                // Tạo nền
                val backgroundBitmap = BitmapFactory.decodeResource(context.resources, idBackground)
                    ?: Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                        eraseColor(ContextCompat.getColor(context, idBackground))
                    }
                val scaledBackground = Bitmap.createScaledBitmap(backgroundBitmap, width, height, true)

                // Tạo tên file MP4 với timestamp
                val fileName = "$projectName-${System.currentTimeMillis()}.mp4"

                // Thiết lập ContentValues cho MediaStore
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/StickmanExports")
                }

                // Tạo file trong MediaStore
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: throw Exception("Failed to create MediaStore record")

                // Thiết lập MediaFormat
                val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                    setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                    setInteger(MediaFormat.KEY_BIT_RATE, 2_000_000)
                    setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                    setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                }

                // Khởi tạo MediaCodec
                val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
                try {
                    codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                } catch (e: Exception) {
                    throw Exception("Failed to configure MediaCodec: ${e.message}")
                }
                val inputSurface: Surface = codec.createInputSurface()
                if (!inputSurface.isValid) {
                    throw Exception("Input surface is not valid")
                }
                codec.start()

                // Kiểm tra format của codec
                val codecFormat = codec.outputFormat
                if (codecFormat == null) {
                    throw Exception("MediaCodec output format is null")
                }

                // Khởi tạo MediaMuxer
                resolver.openOutputStream(uri)?.use { outputStream ->
                    val tempFile = File(context.cacheDir, fileName)
                    val muxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                    val videoTrackIndex = muxer.addTrack(codecFormat)
                    muxer.start()

                    val bufferInfo = MediaCodec.BufferInfo()
                    val frameIntervalNanos = (1_000_000_000 / frameRate).toLong() // Thời gian mỗi frame (nanosecond)

                    // Ghi từng frame
                    frames.forEachIndexed { index, frame ->
                        if (!inputSurface.isValid) {
                            throw Exception("Surface is not valid at frame $index")
                        }
                        val canvas = try {
                            inputSurface.lockCanvas(null)
                        } catch (e: Exception) {
                            throw Exception("Failed to lock canvas at frame $index: ${e.message}")
                        }
                        try {
                            canvas.drawBitmap(scaledBackground, 0f, 0f, null)
                            val scaledFrame = if (frame.width != width || frame.height != height) {
                                Bitmap.createScaledBitmap(frame, width, height, true)
                            } else {
                                frame
                            }
                            canvas.drawBitmap(scaledFrame, 0f, 0f, null)
                            inputSurface.unlockCanvasAndPost(canvas)
                            if (scaledFrame != frame) scaledFrame.recycle()
                        } catch (e: Exception) {
                            inputSurface.unlockCanvasAndPost(canvas) // Đảm bảo mở khóa nếu có lỗi
                            throw Exception("Failed to process frame $index: ${e.message}")
                        }

                        // Xử lý output buffer
                        var outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 100000) // Tăng timeout lên 100ms
                        while (outputBufferIndex >= 0) {
                            val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                            if (outputBuffer != null && bufferInfo.size > 0) {
                                bufferInfo.presentationTimeUs = index * frameIntervalNanos / 1000
                                muxer.writeSampleData(videoTrackIndex, outputBuffer, bufferInfo)
                            }
                            codec.releaseOutputBuffer(outputBufferIndex, false)
                            outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 100000)
                        }
                        android.util.Log.d("ExportUtils", "Added MP4 frame $index")
                    }

                    // Chờ thêm để xử lý buffer cuối cùng
                    var outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 100000)
                    while (outputBufferIndex >= 0) {
                        val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                        if (outputBuffer != null && bufferInfo.size > 0) {
                            bufferInfo.presentationTimeUs = frames.size * frameIntervalNanos / 1000
                            muxer.writeSampleData(videoTrackIndex, outputBuffer, bufferInfo)
                        }
                        codec.releaseOutputBuffer(outputBufferIndex, false)
                        outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 100000)
                    }

                    // Kết thúc
                    try {
                        codec.stop()
                    } catch (e: Exception) {
                        android.util.Log.e("ExportUtils", "Failed to stop codec: ${e.message}")
                    }
                    try {
                        codec.release()
                    } catch (e: Exception) {
                        android.util.Log.e("ExportUtils", "Failed to release codec: ${e.message}")
                    }
                    try {
                        muxer.stop()
                        muxer.release()
                    } catch (e: Exception) {
                        android.util.Log.e("ExportUtils", "Failed to stop/release muxer: ${e.message}")
                    }

                    // Copy file tạm vào MediaStore
                    tempFile.inputStream().use { input ->
                        outputStream.write(input.readBytes())
                    }
                    tempFile.delete()
                } ?: throw Exception("Failed to open output stream")

                // Giải phóng backgroundBitmap
                if (!backgroundBitmap.isRecycled) backgroundBitmap.recycle()
                if (!scaledBackground.isRecycled) scaledBackground.recycle()

                // Giải phóng Surface
                if (inputSurface.isValid) {
                    inputSurface.release()
                }

                // Thông báo thành công
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