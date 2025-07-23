package com.hadat.stickman.ui.home

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.hadat.stickman.R
import java.io.File

class PreviewFragment : Fragment() {

    private lateinit var imageView: ImageView
    private lateinit var buttonBack: ImageButton
    private val args: PreviewFragmentArgs by navArgs()
    private var currentImageIndex = 0
    private val handler = Handler(Looper.getMainLooper())
    private var isSlideshowRunning = false
    private var pathList: Array<String>? = null

    private val slideshowRunnable = object : Runnable {
        override fun run() {
            if (isSlideshowRunning && !pathList.isNullOrEmpty()) {
                val path = pathList!![currentImageIndex]
                loadBitmapFromPath(path)?.let {
                    imageView.setImageBitmap(it)
                }
                currentImageIndex = (currentImageIndex + 1) % pathList!!.size
                handler.postDelayed(this, 50)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return inflater.inflate(R.layout.fragment_preview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imageView = view.findViewById(R.id.imageView)
        buttonBack = view.findViewById(R.id.btnStop)

        pathList = args.bitmapPathList

        if (!pathList.isNullOrEmpty()) {
            isSlideshowRunning = true
            currentImageIndex = 0
            // Không hiển thị ảnh đầu tiên ngay lập tức, mà bắt đầu sau 50ms cho đồng bộ
            handler.postDelayed(slideshowRunnable, 50)
        }

        buttonBack.setOnClickListener {
            stopSlideshowAndBack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopSlideshow()
    }

    private fun stopSlideshowAndBack() {
        stopSlideshow()
        findNavController().popBackStack()
    }

    private fun stopSlideshow() {
        isSlideshowRunning = false
        handler.removeCallbacks(slideshowRunnable)
    }

    private fun loadBitmapFromPath(path: String): Bitmap? {
        return try {
            val file = File(path)
            if (file.exists()) BitmapFactory.decodeFile(path) else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
