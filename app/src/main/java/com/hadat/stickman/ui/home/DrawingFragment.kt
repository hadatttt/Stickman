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
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.hadat.stickman.databinding.FragmentDrawingBinding
import com.hadat.stickman.ui.category.DrawingViewModel
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener

/**
 * Fragment để xử lý giao diện vẽ, quan sát trạng thái ViewModel để cập nhật UI.
 */
class DrawingFragment : Fragment() {

    private var _binding: FragmentDrawingBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DrawingViewModel by viewModels()
    private lateinit var drawingView: DrawingView

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
        drawingView = binding.drawingView
        viewModel.setDrawingView(drawingView, binding.backgroundImage)

        // Tải hình nền và thiết lập cho ImageView
        Glide.with(this)
            .asBitmap()
            .load(itemModel.imageUrl)
            .override(drawingView.width, drawingView.height)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    binding.backgroundImage.setImageBitmap(resource)
                    Toast.makeText(requireContext(), "Hình nền được thiết lập", Toast.LENGTH_SHORT).show()
                }
                override fun onLoadCleared(placeholder: Drawable?) {
                    binding.backgroundImage.setImageDrawable(null)
                    Toast.makeText(requireContext(), "Hình nền đã bị xóa", Toast.LENGTH_SHORT).show()
                }
                override fun onLoadFailed(errorDrawable: Drawable?) {
                    binding.backgroundImage.setImageDrawable(null)
                    Toast.makeText(requireContext(), "Không thể tải hình nền", Toast.LENGTH_SHORT).show()
                }
            })

        setupObservers()
        setupToolbar()
        setupColorAndSizeControls()
        setupBottomControls()
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
    }

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

    private fun setupBottomControls() {
        binding.btnBack.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

//        binding.btnPlay.setOnClickListener {
//            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
//        }
//
//        binding.btnCreate.setOnClickListener {
//            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
//            viewModel.saveDrawing()
//        }
    }

    private fun updateButtonStates(selectedButton: View) {
        listOf(binding.btnBrush, binding.btnEraser, binding.btnFill, binding.btnRectangle, binding.btnCircle).forEach {
            it.isSelected = it == selectedButton
            it.alpha = if (it.isSelected) 1f else 0.5f
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}