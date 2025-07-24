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
import android.widget.Toast
import com.hadat.stickman.ui.database.AppDatabase
import com.hadat.stickman.ui.database.ProjectEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

object ExportUtils {

    private var projectDao: com.hadat.stickman.ui.database.ProjectDao? = null

    // Gọi hàm này 1 lần trước khi sử dụng export
    fun initialize(context: Context) {
        val db = androidx.room.Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "stickman_database"
        ).build()
        projectDao = db.projectDao()
    }
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
                    var trackIndex = -1
                    var muxerStarted = false

                    val bufferInfo = MediaCodec.BufferInfo()
                    val frameIntervalNanos = 1_000_000_000L / frameRate

                    var frameIndex = 0L
                    val startTime = System.nanoTime()

                    frames.forEachIndexed { index, frame ->
                        val canvas = inputSurface.lockCanvas(null)
                        canvas.drawBitmap(scaledBackground, 0f, 0f, null)

                        val scaledFrame = if (frame.width != width || frame.height != height) {
                            Bitmap.createScaledBitmap(frame, width, height, true)
                        } else frame

                        canvas.drawBitmap(scaledFrame, 0f, 0f, null)
                        inputSurface.unlockCanvasAndPost(canvas)

                        if (scaledFrame != frame) scaledFrame.recycle()

                        // Đợi để đảm bảo encoder xử lý frame trước
                        Thread.sleep((1000L / frameRate).coerceAtLeast(15L))
                    }

                    // Báo hiệu không còn frame
                    codec.signalEndOfInputStream()

                    // Drain toàn bộ dữ liệu
                    var outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 100_000)
                    while (outputBufferIndex != MediaCodec.INFO_TRY_AGAIN_LATER) {
                        if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            if (muxerStarted) throw RuntimeException("Format changed twice")
                            val newFormat = codec.outputFormat
                            trackIndex = muxer.addTrack(newFormat)
                            muxer.start()
                            muxerStarted = true
                        } else if (outputBufferIndex >= 0) {
                            val encodedData = codec.getOutputBuffer(outputBufferIndex)
                                ?: throw RuntimeException("Encoder output buffer $outputBufferIndex was null")

                            if (bufferInfo.size > 0) {
                                encodedData.position(bufferInfo.offset)
                                encodedData.limit(bufferInfo.offset + bufferInfo.size)
                                bufferInfo.presentationTimeUs = frameIndex * frameIntervalNanos / 1000
                                frameIndex++
                                muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                            }

                            codec.releaseOutputBuffer(outputBufferIndex, false)
                        }
                        outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 100_000)
                    }

                    codec.stop()
                    codec.release()
                    muxer.stop()
                    muxer.release()

                    // Copy từ file tạm ra MediaStore
                    tempFile.inputStream().use { input -> outputStream.write(input.readBytes()) }
                    tempFile.delete()
                } ?: throw Exception("Failed to open output stream")

                backgroundBitmap.safeRecycle()
                scaledBackground.safeRecycle()
                inputSurface.release()

                // Lưu vào DB
                val project = ProjectEntity(
                    id = generateProjectId(),
                    name = projectName,
                    videoUrl = uri.toString()
                )
                projectDao?.let { dao ->
                    withContext(Dispatchers.IO) {
                        dao.insert(project)
                    }
                } ?: throw Exception("ProjectDao not initialized")

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "MP4 exported successfully to $fileName", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "MP4 export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }


    private suspend fun generateProjectId(): Int {
        return projectDao?.getMaxId()?.let { (it ?: 0) + 1 } ?: 1
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
                    BitmapFactory.decodeStream(inputStream) ?: createDefaultBackground(width, height)
                }
            } catch (e: Exception) {
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
