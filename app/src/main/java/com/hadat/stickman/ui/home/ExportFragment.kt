package com.hadat.stickman.ui.home

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.hadat.stickman.R
import com.hadat.stickman.databinding.FragmentExportBinding
import com.hadat.stickman.ui.utils.ExportUtils
import java.io.File

class ExportFragment : Fragment() {

    private var _binding: FragmentExportBinding? = null
    private val binding get() = _binding!!

    private val args: ExportFragmentArgs by navArgs()

    private var selectedFormat: String = "mp4"
    private var idBackground: Int = R.color.white  // background mặc định trắng

    private var bitmapPathList: List<String> = emptyList()
    // Lưu bitmapList chỉ khi cần xuất, không load toàn bộ lúc onViewCreated

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Lấy danh sách đường dẫn bitmap từ args
        bitmapPathList = args.bitmapPathList?.toList() ?: emptyList()

        // Lấy idBackground từ args, nếu bằng 0 hoặc -1 thì dùng mặc định trắng
        idBackground = args.idBackground.takeIf { it > 0 } ?: R.color.white

        // Load ảnh nền
        val drawable: Drawable? = ContextCompat.getDrawable(requireContext(), idBackground)
        binding.imageBackground.setImageDrawable(drawable)

        setupSpinners()
        setupFormatSelection()
        setupBackgroundSelector()
        setupCreateButton()
    }

    private fun setupSpinners() {
        val aspectRatios = listOf("1:1", "16:9", "4:3", "3:4")
        val frameRates = listOf("12 fps", "24 fps", "30 fps", "60 fps")

        binding.spinnerAspectRatio.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            aspectRatios
        )

        binding.spinnerFrameRate.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            frameRates
        )
    }

    private fun setupFormatSelection() {
        binding.radioGroupFormat.setOnCheckedChangeListener { _, checkedId ->
            selectedFormat = when (checkedId) {
                R.id.radioGif -> "gif"
                else -> "mp4"
            }
        }
        // Đặt mặc định là mp4
        binding.radioGroupFormat.check(R.id.radioMp4)
    }

    private fun setupBackgroundSelector() {
        binding.imageBackground.setOnClickListener {
            val action = ExportFragmentDirections.actionExportFragmentToBackgroundSelectionFragment()
            findNavController().navigate(action)
        }
    }

    private fun setupCreateButton() {
        binding.buttonCreate.setOnClickListener {
            val projectName = binding.editTextProjectName.text.toString().ifBlank { "MyProject" }
            val frameRate = binding.spinnerFrameRate.selectedItem.toString()
                .replace(" fps", "").toIntOrNull() ?: 24

            if (bitmapPathList.isEmpty()) {
                Toast.makeText(requireContext(), "No frames to export", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Khi cần xuất mới decode bitmap từ đường dẫn (giảm tải bộ nhớ)
            val bitmapList = bitmapPathList.mapNotNull { path ->
                val file = File(path)
                if (file.exists()) {
                    BitmapFactory.decodeFile(path)
                } else {
                    null
                }
            }

            if (bitmapList.isEmpty()) {
                Toast.makeText(requireContext(), "No valid frames found", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            when (selectedFormat) {
                "mp4" -> ExportUtils.exportToMp4(requireContext(), bitmapList, frameRate, projectName, R.drawable.bg2)
                "gif" -> ExportUtils.exportToGif(requireContext(), bitmapList, frameRate, projectName, R.drawable.bg1)
            }

            Toast.makeText(requireContext(), "Exported as $selectedFormat", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
