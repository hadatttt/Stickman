package com.hadat.stickman.ui.utils

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import java.io.IOException

class VideoWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return VideoWallpaperEngine()
    }

    inner class VideoWallpaperEngine : Engine() {

        private var mediaPlayer: MediaPlayer? = null
        private var isVisible = false

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            initializeMediaPlayer(holder)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            // Cập nhật lại surface cho MediaPlayer khi surface thay đổi
            mediaPlayer?.setSurface(holder.surface)
        }

        private fun initializeMediaPlayer(holder: SurfaceHolder) {
            // Giải phóng MediaPlayer cũ nếu tồn tại
            mediaPlayer?.release()
            mediaPlayer = null

            val prefs = getSharedPreferences("VideoWallpaperPrefs", Context.MODE_PRIVATE)
            val videoUriString = prefs.getString("videoUri", null) ?: return

            try {
                val videoUri = Uri.parse(videoUriString)
                mediaPlayer = MediaPlayer().apply {
                    setSurface(holder.surface)
                    setDataSource(applicationContext, videoUri)
                    isLooping = true
                    setVolume(0f, 0f) // Tắt âm thanh
                    setOnPreparedListener {
                        if (isVisible) start()
                    }
                    setOnErrorListener { _, what, extra ->
                        // Ghi log lỗi để debug
                        println("MediaPlayer error: what=$what, extra=$extra")
                        true
                    }
                    try {
                        prepare()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            isVisible = visible
            mediaPlayer?.let {
                try {
                    if (visible && it.isPrepared()) {
                        it.start()
                    } else {
                        it.pause()
                    }
                } catch (e: IllegalStateException) {
                    e.printStackTrace()
                    // Thử khởi tạo lại MediaPlayer nếu trạng thái không hợp lệ
                    initializeMediaPlayer(surfaceHolder)
                }
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            mediaPlayer?.pause()
            mediaPlayer?.release()
            mediaPlayer = null
        }

        override fun onDestroy() {
            super.onDestroy()
            mediaPlayer?.release()
            mediaPlayer = null
        }

        // Hàm tiện ích để kiểm tra xem MediaPlayer đã sẵn sàng chưa
        private fun MediaPlayer.isPrepared(): Boolean {
            return try {
                isPlaying || duration > 0
            } catch (e: IllegalStateException) {
                false
            }
        }
    }
}