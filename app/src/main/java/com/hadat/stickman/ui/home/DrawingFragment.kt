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
        val imageUrl = itemModel.imageUrl
        drawingView = binding.drawingView

        drawingView.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                drawingView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                viewModel.setDrawingView(drawingView, binding.backgroundImage, 1)

                Glide.with(this@DrawingFragment)
                    .asBitmap()
                    .load(imageUrl.first())
                    .override(drawingView.width, drawingView.height)
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap>?
                        ) {
                            binding.backgroundImage.setImageBitmap(resource)
                            Toast.makeText(
                                requireContext(),
                                "Hình nền được thiết lập",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            binding.backgroundImage.setImageDrawable(null)
                        }

                        override fun onLoadFailed(errorDrawable: Drawable?) {
                            binding.backgroundImage.setImageDrawable(null)
                            Toast.makeText(
                                requireContext(),
                                "Không thể tải hình nền",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
            }
        })

        setupFrameRecyclerView(frameCount)
        setupObservers()
        setupToolbar()
        setupColorAndSizeControls()
        setupBottomControls()
    }

    private fun setupFrameRecyclerView(frameCount: Int) {
        if (frameCount <= 0) {
            Toast.makeText(requireContext(), "Số frame không hợp lệ", Toast.LENGTH_SHORT).show()
            return
        }

        frameList = (1..frameCount).map { id ->
            val drawingState = viewModel.getDrawingList().find { it.id == id }
            FrameModel(id = id, previewBitmap = drawingState?.bitmap)
        }

        frameAdapter = FrameAdapter(frameList) { drawingId ->
            viewModel.switchDrawing(drawingId)
            Toast.makeText(requireContext(), "Đã chuyển sang frame $drawingId", Toast.LENGTH_SHORT)
                .show()
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

        viewModel.toastMessage.observe(viewLifecycleOwner) { message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }

        viewModel.currentDrawingId.observe(viewLifecycleOwner) { drawingId ->
            frameAdapter.updateSelectedPosition(drawingId)
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
                Toast.makeText(requireContext(), "Không có frame nào để gửi", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            // Lưu bitmap vào cache, lấy danh sách đường dẫn
            val bitmapPathList = saveBitmapsToCache(requireContext(), bitmapList)
            // Truyền danh sách đường dẫn qua SafeArgs
            val action =
                DrawingFragmentDirections.actionDrawingFragmentToPreviewFragment(bitmapPathList.toTypedArray())
            findNavController().navigate(action)
        }
        binding.btnFinish.setOnClickListener {
            viewModel.saveCurrentDrawingState(viewModel.currentDrawingId.value ?: 1)
            val drawingList = viewModel.getDrawingList()


            // Lấy danh sách bitmap không null
            val bitmapList = drawingList.mapNotNull { it.bitmap }

            if (bitmapList.isEmpty()) {
                Toast.makeText(requireContext(), "Không có frame nào để gửi", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            // Lưu bitmap vào cache, lấy danh sách đường dẫn
            val bitmapPathList = saveBitmapsToCache(requireContext(), bitmapList)
            // Truyền danh sách đường dẫn qua SafeArgs
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

    // Hàm lưu danh sách Bitmap vào bộ nhớ cache, trả về danh sách đường dẫn file
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