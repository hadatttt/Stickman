package com.hadat.stickman.ui.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.SeekBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.hadat.stickman.databinding.FragmentDrawingBinding
import com.hadat.stickman.ui.category.DrawingViewModel
import com.hadat.stickman.ui.category.FrameAdapter
import com.hadat.stickman.ui.model.FrameModel
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class DrawingFragment : Fragment() {

    private var _binding: FragmentDrawingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DrawingViewModel by viewModels()
    private lateinit var drawingView: DrawingView
    private lateinit var frameAdapter: FrameAdapter
    private lateinit var frameList: MutableList<FrameModel>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDrawingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args: DrawingFragmentArgs by navArgs()
        val itemModel = args.itemModel
        val frameCount = itemModel.frame
        val imageUrls = itemModel.imageUrl
        drawingView = binding.drawingView

        // Pass the list of image URLs to the ViewModel
        viewModel.setImageUrls(imageUrls)

        // Load initial sticker for DrawingView
        val stickerUrl = "https://img.lovepik.com/free-png/20211119/lovepik-qingming-handwritten-style-png-image_401042234_wh1200.png"
        Glide.with(this@DrawingFragment)
            .asBitmap()
            .load(stickerUrl)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    drawingView.setStickerBitmap(resource)
                }
                override fun onLoadCleared(placeholder: Drawable?) {}
                override fun onLoadFailed(errorDrawable: Drawable?) {
                    Log.e("DrawingFragment", "Failed to load sticker from URL")
                }
            })

        drawingView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                drawingView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                viewModel.setDrawingView(drawingView, binding.backgroundImage, 1)
                loadBackgroundImage(1)
            }
        })

        setupFrameRecyclerView(frameCount)
        setupObservers()
        setupToolbar()
        setupColorAndSizeControls()
        setupBottomControls()
    }

    private fun loadBackgroundImage(drawingId: Int) {
        val imageUrl = viewModel.getImageUrlForDrawing(drawingId)
        if (imageUrl != null) {
            Glide.with(this@DrawingFragment)
                .asBitmap()
                .load(imageUrl)
                .override(drawingView.width, drawingView.height)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        binding.backgroundImage.setImageBitmap(resource)
                    }
                    override fun onLoadCleared(placeholder: Drawable?) {
                        binding.backgroundImage.setImageDrawable(null)
                    }
                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        binding.backgroundImage.setImageDrawable(null)
                        Log.e("DrawingFragment", "Failed to load background image for drawingId: $drawingId")
                    }
                })
        } else {
            binding.backgroundImage.setImageDrawable(null)
        }
    }

    private fun setupFrameRecyclerView(frameCount: Int) {
        // Initialize frameList from ViewModel to restore all frames
        frameList = viewModel.getDrawingList().map { drawingState ->
            FrameModel(id = drawingState.id, previewBitmap = drawingState.bitmap?.copy(Bitmap.Config.ARGB_8888, true))
        }.toMutableList()

        // If frameList is empty and frameCount > 0, initialize with frameCount
        if (frameList.isEmpty() && frameCount > 0) {
            frameList = (1..frameCount).map { id ->
                val drawingState = viewModel.getDrawingList().find { it.id == id }
                FrameModel(id = id, previewBitmap = drawingState?.bitmap?.copy(Bitmap.Config.ARGB_8888, true))
            }.toMutableList()
            // Ensure initial frames are added to ViewModel
            frameList.forEach { frame ->
                if (!viewModel.getDrawingList().any { it.id == frame.id }) {
                    viewModel.addNewDrawing(frame.id)
                }
            }
        }

        frameAdapter = FrameAdapter(frameList) { drawingId ->
            viewModel.saveCurrentDrawingState(viewModel.currentDrawingId.value ?: 1)
            viewModel.switchDrawing(drawingId)
            loadBackgroundImage(drawingId)
            Log.d("DrawingFragment", "Switched to frame: $drawingId")
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = frameAdapter
        }

        viewModel.drawingList.observe(viewLifecycleOwner) { drawingList ->
            // Rebuild frameList to include all frames
            frameList.clear()
            frameList.addAll(drawingList.map { drawingState ->
                FrameModel(id = drawingState.id, previewBitmap = drawingState.bitmap?.copy(Bitmap.Config.ARGB_8888, true))
            })
            frameAdapter.notifyDataSetChanged()
            Log.d("DrawingFragment", "Drawing list updated, size: ${drawingList.size}")
        }
    }

    private fun setupObservers() {
        viewModel.mode.observe(viewLifecycleOwner) { mode ->
            updateButtonStates(
                when (mode) {
                    DrawingView.Mode.DRAW -> binding.btnBrush
                    DrawingView.Mode.ERASE -> binding.btnEraser
                    DrawingView.Mode.FILL -> binding.btnFill
                    DrawingView.Mode.RECTANGLE -> binding.btnRectangle
                    DrawingView.Mode.CIRCLE -> binding.btnCircle
                    DrawingView.Mode.LINE -> binding.btnLine
                    DrawingView.Mode.STICKER -> binding.btnSticker
                }
            )
            val (size, max) = viewModel.getSizeForMode()
            binding.seekBarStrokeSize.max = max
            binding.seekBarStrokeSize.progress = size
        }

        viewModel.color.observe(viewLifecycleOwner) { color ->
            binding.colorPreview.setBackgroundColor(color)
        }

        viewModel.opacity.observe(viewLifecycleOwner) { opacity ->
            binding.seekBarOpacity.progress = opacity
        }

        viewModel.currentDrawingId.observe(viewLifecycleOwner) { drawingId ->
            frameAdapter.updateSelectedPosition(drawingId)
            loadBackgroundImage(drawingId)
            Log.d("DrawingFragment", "Current drawing ID changed to: $drawingId")
        }
    }

    private fun setupToolbar() {
        binding.btnBrush.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            viewModel.setMode(DrawingView.Mode.DRAW, viewModel.currentDrawingId.value ?: 1)
        }

        binding.btnEraser.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            viewModel.setMode(DrawingView.Mode.ERASE, viewModel.currentDrawingId.value ?: 1)
        }

        binding.btnFill.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            viewModel.setMode(DrawingView.Mode.FILL, viewModel.currentDrawingId.value ?: 1)
        }

        binding.btnRectangle.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            viewModel.setMode(DrawingView.Mode.RECTANGLE, viewModel.currentDrawingId.value ?: 1)
        }

        binding.btnCircle.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            viewModel.setMode(DrawingView.Mode.CIRCLE, viewModel.currentDrawingId.value ?: 1)
        }

        binding.btnLine.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            viewModel.setMode(DrawingView.Mode.LINE, viewModel.currentDrawingId.value ?: 1)
        }

        binding.btnSticker.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            viewModel.setMode(DrawingView.Mode.STICKER, viewModel.currentDrawingId.value ?: 1)
        }

        binding.btnUndo.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            viewModel.undo(viewModel.currentDrawingId.value ?: 1)
        }

        binding.btnRedo.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            viewModel.redo(viewModel.currentDrawingId.value ?: 1)
        }

        binding.btnClear.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            viewModel.clearDrawing(viewModel.currentDrawingId.value ?: 1)
        }
    }

    private fun setupColorAndSizeControls() {
        binding.colorPreview.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            ColorPickerDialog.Builder(requireContext())
                .setTitle("Chọn màu")
                .setPositiveButton("Chọn", ColorEnvelopeListener { envelope, _ ->
                    viewModel.setColor(envelope.color, viewModel.currentDrawingId.value ?: 1)
                })
                .setNegativeButton("Hủy") { dialog, _ -> dialog.dismiss() }
                .attachAlphaSlideBar(true)
                .attachBrightnessSlideBar(true)
                .setBottomSpace(12)
                .show()
        }

        binding.seekBarStrokeSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) viewModel.setSize(progress, viewModel.currentDrawingId.value ?: 1)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.seekBarOpacity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) viewModel.setOpacity(progress, viewModel.currentDrawingId.value ?: 1)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupBottomControls() {
        binding.btnBack.setOnClickListener {
            // Save current drawing state before navigating back
            viewModel.saveCurrentDrawingState(viewModel.currentDrawingId.value ?: 1)
            Log.d("DrawingFragment", "Saved current drawing state before popBackStack")
            findNavController().popBackStack()
        }

        binding.btnPreview.setOnClickListener {
            viewModel.saveCurrentDrawingState(viewModel.currentDrawingId.value ?: 1)
            val drawingList = viewModel.getDrawingList()
            val bitmapList = drawingList.mapNotNull { it.bitmap }
            if (bitmapList.isEmpty()) {
                Toast.makeText(requireContext(), "Không có bản vẽ để xem trước!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val bitmapPathList = saveBitmapsToCache(requireContext(), bitmapList)
            val action = DrawingFragmentDirections.actionDrawingFragmentToPreviewFragment(bitmapPathList.toTypedArray())
            findNavController().navigate(action)
        }

        binding.btnFinish.setOnClickListener {
            viewModel.saveCurrentDrawingState(viewModel.currentDrawingId.value ?: 1)
            val drawingList = viewModel.getDrawingList()
            val bitmapList = drawingList.mapNotNull { it.bitmap }
            if (bitmapList.isEmpty()) {
                Toast.makeText(requireContext(), "Không có bản vẽ để xuất!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val bitmapPathList = saveBitmapsToCache(requireContext(), bitmapList)
            val action = DrawingFragmentDirections.actionDrawingFragmentToExportFragment(bitmapPathList.toTypedArray())
            findNavController().navigate(action)
        }

        binding.imgAddFrame.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            addNewFrame()
        }
    }

    private fun addNewFrame() {
        // Save current drawing state before adding new frame
        viewModel.saveCurrentDrawingState(viewModel.currentDrawingId.value ?: 1)
        val newFrameId = (frameList.maxByOrNull { it.id }?.id ?: 0) + 1
        val newFrame = FrameModel(id = newFrameId, previewBitmap = null)
        frameList.add(newFrame)
        viewModel.addNewDrawing(newFrameId)
        frameAdapter.notifyItemInserted(frameList.size - 1)
        binding.recyclerView.scrollToPosition(frameList.size - 1)
        viewModel.switchDrawing(newFrameId)
        loadBackgroundImage(newFrameId)
        Log.d("DrawingFragment", "Added new frame with ID: $newFrameId, frameList size: ${frameList.size}")
    }

    private fun updateButtonStates(selectedButton: View) {
        listOf(
            binding.btnBrush,
            binding.btnEraser,
            binding.btnFill,
            binding.btnRectangle,
            binding.btnCircle,
            binding.btnLine,
            binding.btnSticker
        ).forEach {
            it.isSelected = it == selectedButton
            it.alpha = if (it.isSelected) 1f else 0.5f
        }
    }

    private fun saveBitmapsToCache(context: Context, bitmaps: List<Bitmap>): List<String> {
        val paths = mutableListOf<String>()
        bitmaps.forEachIndexed { index, bitmap ->
            try {
                if (bitmap == null) {
                    // Create a default white bitmap if null
                    val defaultBitmap = Bitmap.createBitmap(
                        drawingView.width, drawingView.height, Bitmap.Config.ARGB_8888
                    )
                    val canvas = Canvas(defaultBitmap)
                    canvas.drawColor(Color.WHITE)
                    val file = File(context.cacheDir, "drawing_frame_$index.png")
                    FileOutputStream(file).use { out ->
                        defaultBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        out.flush()
                    }
                    paths.add(file.absolutePath)
                    Log.d("DrawingFragment", "Saved default bitmap for frame $index")
                } else {
                    val file = File(context.cacheDir, "drawing_frame_$index.png")
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        out.flush()
                    }
                    paths.add(file.absolutePath)
                    Log.d("DrawingFragment", "Saved bitmap for frame $index")
                }
            } catch (e: IOException) {
                Log.e("DrawingFragment", "Error saving bitmap for frame $index: ${e.message}")
                e.printStackTrace()
            }
        }
        return paths
    }

    override fun onDestroyView() {
        // Save current drawing state before destroying view
        viewModel.saveCurrentDrawingState(viewModel.currentDrawingId.value ?: 1)
        Log.d("DrawingFragment", "Saved current drawing state in onDestroyView")
        super.onDestroyView()
        _binding = null
    }
}