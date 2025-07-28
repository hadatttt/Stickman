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
import android.widget.PopupWindow
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
import com.hadat.stickman.databinding.LayoutShapeDropdownBinding
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
    private var popupWindow: PopupWindow? = null

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
        val imageUrls = args.imageUrls
        val frameCount = imageUrls.size
        drawingView = binding.drawingView

        viewModel.setImageUrls(imageUrls)

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
                    Log.e("DrawingFragment", "Không thể tải sticker từ URL")
                }
            })

        drawingView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                drawingView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                viewModel.setDrawingView(drawingView, binding.backgroundImage, 1)
                loadBackgroundImage(1)
            }
        })
        drawingView.onColorPicked = { color ->
            viewModel.setColor(color, viewModel.currentDrawingId.value ?: 1)
        }
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
                        Log.e("DrawingFragment", "Không thể tải ảnh nền cho drawingId: $drawingId")
                    }
                })
        } else {
            binding.backgroundImage.setImageDrawable(null)
        }
    }

    private fun setupFrameRecyclerView(frameCount: Int) {
        frameList = viewModel.getDrawingList().map { drawingState ->
            FrameModel(id = drawingState.id, previewBitmap = drawingState.bitmap?.copy(Bitmap.Config.ARGB_8888, true))
        }.toMutableList()

        if (frameList.isEmpty() && frameCount > 0) {
            frameList = (1..frameCount).map { id ->
                val drawingState = viewModel.getDrawingList().find { it.id == id }
                FrameModel(id = id, previewBitmap = drawingState?.bitmap?.copy(Bitmap.Config.ARGB_8888, true))
            }.toMutableList()
            frameList.forEach { frame ->
                if (!viewModel.getDrawingList().any { it.id == frame.id }) {
                    viewModel.addNewDrawing(frame.id)
                }
            }
        }

        frameAdapter = FrameAdapter(frameList, { drawingId ->
            viewModel.saveCurrentDrawingState(viewModel.currentDrawingId.value ?: 1)
            viewModel.switchDrawing(drawingId)
            loadBackgroundImage(drawingId)
            Log.d("DrawingFragment", "Đã chuyển sang frame: $drawingId")
        }, {
            addNewFrame()
        })

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = frameAdapter
        }

        viewModel.drawingList.observe(viewLifecycleOwner) { drawingList ->
            frameList.clear()
            frameList.addAll(drawingList.map { drawingState ->
                FrameModel(id = drawingState.id, previewBitmap = drawingState.bitmap?.copy(Bitmap.Config.ARGB_8888, true))
            })
            frameAdapter.notifyDataSetChanged()
            Log.d("DrawingFragment", "Danh sách frame đã cập nhật, kích thước: ${drawingList.size}")
        }
    }

    private fun setupObservers() {
        viewModel.mode.observe(viewLifecycleOwner) { mode ->
            updateButtonStates(
                when (mode) {
                    DrawingView.Mode.DRAW -> binding.btnBrush
                    DrawingView.Mode.ERASE -> binding.btnEraser
                    DrawingView.Mode.FILL -> binding.btnFill
                    DrawingView.Mode.RECTANGLE -> {
                        binding.btnShapes.setImageResource(com.hadat.stickman.R.drawable.bg1)
                        binding.btnShapes
                    }
                    DrawingView.Mode.CIRCLE -> {
                        binding.btnShapes.setImageResource(com.hadat.stickman.R.drawable.bg2)
                        binding.btnShapes
                    }
                    DrawingView.Mode.LINE -> {
                        binding.btnShapes.setImageResource(com.hadat.stickman.R.drawable.bg1)
                        binding.btnShapes
                    }
                    DrawingView.Mode.STICKER -> binding.btnSticker
                    DrawingView.Mode.COLOR_PICKER -> binding.btnColorPicker
                }
            )
            val (size, max) = viewModel.getSizeForMode()
            binding.seekBarStrokeSize.max = max
            binding.seekBarStrokeSize.progress = size
        }

        viewModel.color.observe(viewLifecycleOwner) { color ->
            Log.d("DrawingFragment", "Observer nhận màu mới: $color")
            binding.colorPreview.setBackgroundColor(color)
        }

        viewModel.opacity.observe(viewLifecycleOwner) { opacity ->
            Log.d("DrawingFragment", "Observer nhận opacity mới: $opacity")
            binding.seekBarOpacity.progress = opacity
        }

        viewModel.currentDrawingId.observe(viewLifecycleOwner) { drawingId ->
            frameAdapter.updateSelectedPosition(drawingId)
            loadBackgroundImage(drawingId)
            Log.d("DrawingFragment", "ID bản vẽ hiện tại thay đổi thành: $drawingId")
        }
    }

    private fun setupToolbar() {
        binding.btnBrush.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            viewModel.setMode(DrawingView.Mode.DRAW, viewModel.currentDrawingId.value ?: 1)
            popupWindow?.dismiss()
        }

        binding.btnEraser.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            viewModel.setMode(DrawingView.Mode.ERASE, viewModel.currentDrawingId.value ?: 1)
            popupWindow?.dismiss()
        }

        binding.btnFill.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            viewModel.setMode(DrawingView.Mode.FILL, viewModel.currentDrawingId.value ?: 1)
            popupWindow?.dismiss()
        }

        binding.btnShapes.setOnClickListener { view ->
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            showShapeDropdown(view)
        }

        binding.btnSticker.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            viewModel.setMode(DrawingView.Mode.STICKER, viewModel.currentDrawingId.value ?: 1)
            popupWindow?.dismiss()
        }

        binding.btnColorPicker.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            viewModel.setMode(DrawingView.Mode.COLOR_PICKER, viewModel.currentDrawingId.value ?: 1)
            popupWindow?.dismiss()
        }

        binding.btnUndo.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            viewModel.undo(viewModel.currentDrawingId.value ?: 1)
            popupWindow?.dismiss()
        }

        binding.btnRedo.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            viewModel.redo(viewModel.currentDrawingId.value ?: 1)
            popupWindow?.dismiss()
        }

        binding.btnClear.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            viewModel.clearDrawing(viewModel.currentDrawingId.value ?: 1)
            popupWindow?.dismiss()
        }
    }

    private fun showShapeDropdown(anchor: View) {
        popupWindow?.dismiss()

        val dropdownBinding = LayoutShapeDropdownBinding.inflate(LayoutInflater.from(requireContext()))
        val popupView = dropdownBinding.root

        popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            setBackgroundDrawable(null)
            elevation = 4f
        }

        dropdownBinding.btnDropdownRectangle.setOnClickListener {
            viewModel.setMode(DrawingView.Mode.RECTANGLE, viewModel.currentDrawingId.value ?: 1)
            popupWindow?.dismiss()
        }
        dropdownBinding.btnDropdownCircle.setOnClickListener {
            viewModel.setMode(DrawingView.Mode.CIRCLE, viewModel.currentDrawingId.value ?: 1)
            popupWindow?.dismiss()
        }
        dropdownBinding.btnDropdownLine.setOnClickListener {
            viewModel.setMode(DrawingView.Mode.LINE, viewModel.currentDrawingId.value ?: 1)
            popupWindow?.dismiss()
        }

        popupWindow?.showAsDropDown(anchor)
    }

    private fun setupColorAndSizeControls() {
        binding.colorPreview.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            ColorPickerDialog.Builder(requireContext())
                .setTitle("Chọn màu")
                .setPositiveButton("Chọn", ColorEnvelopeListener { envelope, _ ->
                    Log.d("DrawingFragment", "Màu được chọn từ ColorPicker: ${envelope.color}")
                    viewModel.setColor(envelope.color, viewModel.currentDrawingId.value ?: 1)
                    binding.colorPreview.setBackgroundColor(envelope.color) // Cập nhật trực tiếp
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
            viewModel.saveCurrentDrawingState(viewModel.currentDrawingId.value ?: 1)
            val action = DrawingFragmentDirections.actionDrawingFragmentToHomeFragment()
            findNavController().navigate(action)
            popupWindow?.dismiss()
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
            popupWindow?.dismiss()
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
            popupWindow?.dismiss()
        }
    }

    private fun addNewFrame() {
        viewModel.saveCurrentDrawingState(viewModel.currentDrawingId.value ?: 1)
        val newFrameId = (frameList.maxByOrNull { it.id }?.id ?: 0) + 1
        val newFrame = FrameModel(id = newFrameId, previewBitmap = null)
        frameList.add(newFrame)
        viewModel.addNewDrawing(newFrameId)
        frameAdapter.notifyItemInserted(frameList.size - 1)
        binding.recyclerView.scrollToPosition(frameList.size)
        viewModel.switchDrawing(newFrameId)
        loadBackgroundImage(newFrameId)
        Log.d("DrawingFragment", "Đã thêm frame mới với ID: $newFrameId, kích thước frameList: ${frameList.size}")
    }

    private fun updateButtonStates(selectedButton: View) {
        listOf(
            binding.btnBrush,
            binding.btnEraser,
            binding.btnFill,
            binding.btnShapes,
            binding.btnSticker,
            binding.btnColorPicker
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
                    Log.d("DrawingFragment", "Đã lưu bitmap mặc định cho frame $index")
                } else {
                    val file = File(context.cacheDir, "drawing_frame_$index.png")
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        out.flush()
                    }
                    paths.add(file.absolutePath)
                    Log.d("DrawingFragment", "Đã lưu bitmap cho frame $index")
                }
            } catch (e: IOException) {
                Log.e("DrawingFragment", "Lỗi khi lưu bitmap cho frame $index: ${e.message}")
                e.printStackTrace()
            }
        }
        return paths
    }

    override fun onDestroyView() {
        viewModel.saveCurrentDrawingState(viewModel.currentDrawingId.value ?: 1)
        popupWindow?.dismiss()
        Log.d("DrawingFragment", "Đã lưu trạng thái bản vẽ hiện tại trong onDestroyView")
        super.onDestroyView()
        _binding = null
    }
}