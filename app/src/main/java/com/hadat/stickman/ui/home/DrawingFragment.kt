package com.hadat.stickman.ui.home

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.SeekBar
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
    private lateinit var frameList: List<FrameModel>

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
        val imageUrls = itemModel.imageUrl // Now a list of strings
        drawingView = binding.drawingView

        // Pass the list of image URLs to the ViewModel
        viewModel.setImageUrls(imageUrls)

        drawingView.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                drawingView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                viewModel.setDrawingView(drawingView, binding.backgroundImage, 1)

                // Load initial background image for drawingId 1
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
                    }
                })
        } else {
            binding.backgroundImage.setImageDrawable(null)
        }
    }

    private fun setupFrameRecyclerView(frameCount: Int) {
        if (frameCount <= 0) {
            return
        }

        frameList = (1..frameCount).map { id ->
            val drawingState = viewModel.getDrawingList().find { it.id == id }
            FrameModel(id = id, previewBitmap = drawingState?.bitmap)
        }

        frameAdapter = FrameAdapter(frameList) { drawingId ->
            viewModel.switchDrawing(drawingId)
            loadBackgroundImage(drawingId) // Load background when switching frames
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = frameAdapter
        }

        viewModel.drawingList.observe(viewLifecycleOwner) { drawingList ->
            frameList.forEachIndexed { index, frame ->
                val drawingState = drawingList.find { it.id == frame.id }
                frame.previewBitmap = drawingState?.bitmap?.copy(Bitmap.Config.ARGB_8888, true)
            }
            frameAdapter.notifyDataSetChanged()
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
            loadBackgroundImage(drawingId) // Update background when drawing ID changes
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

        binding.seekBarStrokeSize.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
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
            val action = DrawingFragmentDirections.actionDrawingFragmentToHomeFragment()
            findNavController().navigate(action)
        }

        binding.btnPreview.setOnClickListener {
            viewModel.saveCurrentDrawingState(viewModel.currentDrawingId.value ?: 1)
            val drawingList = viewModel.getDrawingList()

            val bitmapList = drawingList.mapNotNull { it.bitmap }

            if (bitmapList.isEmpty()) {
                return@setOnClickListener
            }
            val bitmapPathList = saveBitmapsToCache(requireContext(), bitmapList)
            val action =
                DrawingFragmentDirections.actionDrawingFragmentToPreviewFragment(bitmapPathList.toTypedArray())
            findNavController().navigate(action)
        }

        binding.btnFinish.setOnClickListener {
            viewModel.saveCurrentDrawingState(viewModel.currentDrawingId.value ?: 1)
            val drawingList = viewModel.getDrawingList()

            val bitmapList = drawingList.mapNotNull { it.bitmap }

            if (bitmapList.isEmpty()) {
                return@setOnClickListener
            }
            val bitmapPathList = saveBitmapsToCache(requireContext(), bitmapList)
            val action =
                DrawingFragmentDirections.actionDrawingFragmentToExportFragment(bitmapPathList.toTypedArray())
            findNavController().navigate(action)
        }
    }

    private fun updateButtonStates(selectedButton: View) {
        listOf(
            binding.btnBrush,
            binding.btnEraser,
            binding.btnFill,
            binding.btnRectangle,
            binding.btnCircle
        ).forEach {
            it.isSelected = it == selectedButton
            it.alpha = if (it.isSelected) 1f else 0.5f
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun saveBitmapsToCache(
        context: android.content.Context,
        bitmaps: List<Bitmap>
    ): List<String> {
        val paths = mutableListOf<String>()
        bitmaps.forEachIndexed { index, bitmap ->
            try {
                val file = File(context.cacheDir, "drawing_frame_$index.png")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    out.flush()
                }
                paths.add(file.absolutePath)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return paths
    }
}