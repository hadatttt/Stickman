package com.hadat.stickman.ui.home

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.hadat.stickman.databinding.FragmentPreviewVideoBinding
import com.hadat.stickman.ui.utils.VideoWallpaperService

class PreviewVideoFragment : Fragment() {

    private var _binding: FragmentPreviewVideoBinding? = null
    private val binding get() = _binding!!

    private val args: PreviewVideoFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPreviewVideoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.projectNameTextView.text = args.name

        val videoUri = Uri.parse(args.videoUrl)

        // Phát video lặp, không cho điều khiển
        binding.videoView.apply {
            setVideoURI(videoUri)
            setOnPreparedListener { it.isLooping = true }
            start()
        }

        // Khi bấm nút "Đặt làm hình nền"
        binding.setWallpaperButton.setOnClickListener {
            try {
                // Đặt lại hình nền về mặc định để tránh xung đột
                WallpaperManager.getInstance(requireContext()).clear()

                // Lưu videoUri vào SharedPreferences
                val prefs = requireContext().getSharedPreferences("VideoWallpaperPrefs", Context.MODE_PRIVATE)
                prefs.edit().clear().putString("videoUri", args.videoUrl).apply()

                // Ghi log để kiểm tra
                println("Saved videoUri: ${args.videoUrl}")

                // Mở giao diện chọn Live Wallpaper
                val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                    putExtra(
                        WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                        ComponentName(requireContext(), VideoWallpaperService::class.java)
                    )
                }
                startActivity(intent)

                Toast.makeText(requireContext(), "Đang đặt hình nền...", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Lỗi khi đặt hình nền: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}