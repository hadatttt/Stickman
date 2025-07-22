package com.hadat.stickman.ui.home

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

/**
 * Fragment xử lý giao diện vẽ, quan sát trạng thái ViewModel để cập nhật UI.
 */
class DrawingFragment : Fragment() {

    private var _binding: FragmentDrawingBinding? = null
    private val binding get() = _binding!!

    // ViewModel riêng của fragment này
    private val viewModel: DrawingViewModel by viewModels()

    private lateinit var drawingView: DrawingView
    private lateinit var frameAdapter: FrameAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate layout bằng ViewBinding
        _binding = FragmentDrawingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Lấy dữ liệu truyền vào (ItemModel)
        val args: DrawingFragmentArgs by navArgs()
        val itemModel = args.itemModel

        val frameCount = itemModel.frame
        val imageUrl = itemModel.imageUrl
        drawingView = binding.drawingView

        // Thiết lập DrawingView và BackgroundImage cho ViewModel xử lý
        viewModel.setDrawingView(drawingView, binding.backgroundImage)

        // Tải ảnh nền với Glide và gán vào ImageView background
        Glide.with(this)
            .asBitmap()
            .load(imageUrl)
            .override(drawingView.width, drawingView.height)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    binding.backgroundImage.setImageBitmap(resource)
                    Toast.makeText(requireContext(), "Hình nền được thiết lập", Toast.LENGTH_SHORT).show()
                }
                override fun onLoadCleared(placeholder: Drawable?) {
                    binding.backgroundImage.setImageDrawable(null)
                }
                override fun onLoadFailed(errorDrawable: Drawable?) {
                    binding.backgroundImage.setImageDrawable(null)
                    Toast.makeText(requireContext(), "Không thể tải hình nền", Toast.LENGTH_SHORT).show()
                }
            })

        // Thiết lập RecyclerView với FrameAdapter
        setupFrameRecyclerView(frameCount)

        setupObservers()
        setupToolbar()
        setupColorAndSizeControls()
        setupBottomControls()
    }

    /**
     * Thiết lập RecyclerView để hiển thị danh sách FrameModel
     */
    private fun setupFrameRecyclerView(frameCount: Int) {
        // Kiểm tra frameCount hợp lệ
        if (frameCount <= 0) {
            Toast.makeText(requireContext(), "Số frame không hợp lệ", Toast.LENGTH_SHORT).show()
            return
        }

        // Tạo danh sách FrameModel với ID tăng dần từ 1 đến frameCount
        val frameList = (1..frameCount).map { id ->
            FrameModel(id = id, previewBitmap = null)
        }


        // Khởi tạo FrameAdapter
        frameAdapter = FrameAdapter(frameList) { position ->
            // Xử lý khi người dùng chọn một frame
            Toast.makeText(requireContext(), "Đã chọn frame ${frameList[position].id}", Toast.LENGTH_SHORT).show()
            // Có thể thêm logic để chuyển đổi frame trong DrawingView
            // Ví dụ: viewModel.switchToFrame(frameList[position])
        }

        // Thiết lập RecyclerView
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = frameAdapter
        }
    }
    /**
     * Quan sát LiveData trong ViewModel để cập nhật UI
     */
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
    }

    /**
     * Thiết lập các nút trên toolbar để chọn mode, undo/redo, clear
     */
    private fun setupToolbar() {
        binding.btnBrush.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            viewModel.setMode(DrawingView.Mode.DRAW)
        }

        binding.btnEraser.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            viewModel.setMode(DrawingView.Mode.ERASE)
        }

        binding.btnFill.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            viewModel.setMode(DrawingView.Mode.FILL)
        }

        binding.btnRectangle.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            viewModel.setMode(DrawingView.Mode.RECTANGLE)
        }

        binding.btnCircle.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            viewModel.setMode(DrawingView.Mode.CIRCLE)
        }

        binding.btnUndo.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            viewModel.undo()
        }

        binding.btnRedo.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            viewModel.redo()
        }

        binding.btnClear.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            viewModel.clearDrawing()
        }
    }

    /**
     * Thiết lập chọn màu và thay đổi kích thước, độ trong suốt
     */
    private fun setupColorAndSizeControls() {
        binding.colorPreview.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            ColorPickerDialog.Builder(requireContext())
                .setTitle("Chọn màu")
                .setPositiveButton("Chọn", ColorEnvelopeListener { envelope, _ ->
                    viewModel.setColor(envelope.color)
                })
                .setNegativeButton("Hủy") { dialog, _ -> dialog.dismiss() }
                .attachAlphaSlideBar(true)
                .attachBrightnessSlideBar(true)
                .setBottomSpace(12)
                .show()
        }

        binding.seekBarStrokeSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) viewModel.setSize(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.seekBarOpacity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) viewModel.setOpacity(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    /**
     * Nút Back để quay lại HomeFragment
     */
    private fun setupBottomControls() {
        binding.btnBack.setOnClickListener {
            val action = DrawingFragmentDirections.actionDrawingFragmentToHomeFragment()
            findNavController().navigate(action)
        }
    }

    /**
     * Cập nhật trạng thái nút được chọn (highlight)
     */
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
}