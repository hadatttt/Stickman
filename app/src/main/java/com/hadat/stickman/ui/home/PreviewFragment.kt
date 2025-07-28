package com.hadat.stickman.ui.home

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.hadat.stickman.R
import java.io.File

class PreviewFragment : Fragment() {

    private lateinit var imageView: ImageView
    private lateinit var buttonBack: ImageButton
    private lateinit var spinnerFps: Spinner
    private val args: PreviewFragmentArgs by navArgs()
    private var currentImageIndex = 0
    private val handler = Handler(Looper.getMainLooper())
    private var isSlideshowRunning = false
    private var pathList: Array<String>? = null
    private var currentFps = 30 // FPS mặc định

    private val slideshowRunnable = object : Runnable {
        override fun run() {
            if (isSlideshowRunning && !pathList.isNullOrEmpty()) {
                currentImageIndex = (currentImageIndex + 1) % pathList!!.size
                val path = pathList!![currentImageIndex]
                loadBitmapFromPath(path)?.let {
                    imageView.setImageBitmap(it)
                }
                handler.postDelayed(this, (1000 / currentFps).toLong())
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
        spinnerFps = view.findViewById(R.id.spinnerFps)

        // Tạo danh sách FPS động (1 đến 30)
        val fpsList = (1..30).map { "$it FPS" }.toTypedArray()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, fpsList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFps.adapter = adapter
        spinnerFps.setSelection(29) // Chọn 30 FPS làm mặc định

        // Xử lý sự kiện khi chọn FPS
        spinnerFps.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                currentFps = fpsList[position].substringBefore(" FPS").toInt()
                if (isSlideshowRunning) {
                    handler.removeCallbacks(slideshowRunnable)
                    handler.postDelayed(slideshowRunnable, (1000 / currentFps).toLong())
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {
                // Không làm gì nếu không chọn
            }
        })

        pathList = args.bitmapPathList

        if (!pathList.isNullOrEmpty()) {
            isSlideshowRunning = true
            currentImageIndex = 0
            // Hiển thị ảnh đầu tiên ngay lập tức
            val path = pathList!![currentImageIndex]
            loadBitmapFromPath(path)?.let {
                imageView.setImageBitmap(it)
            }
            handler.postDelayed(slideshowRunnable, (1000 / currentFps).toLong())
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
        val startTime = System.currentTimeMillis()
        return try {
            val file = File(path)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(path)
                val duration = System.currentTimeMillis() - startTime
                Log.d("PreviewFragment", "Load bitmap $path took $duration ms")
                bitmap
            } else {
                Log.e("PreviewFragment", "File not found: $path")
                null
            }
        } catch (e: Exception) {
            Log.e("PreviewFragment", "Error loading bitmap $path", e)
            null
        }
    }
}