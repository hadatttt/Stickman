package com.hadat.stickman.ui.home

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.hadat.stickman.R
import com.hadat.stickman.databinding.FragmentExportBinding
import com.hadat.stickman.ui.utils.ExportUtils
import java.io.File

class ExportFragment : Fragment() {

    private var _binding: FragmentExportBinding? = null
    private val binding get() = _binding!!

    private val args: ExportFragmentArgs by navArgs()

    private var selectedFormat: String = "mp4"
    private var backgroundUrl: String = ""

    private var bitmapPathList: List<String> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bitmapPathList = args.bitmapPathList?.toList() ?: emptyList()
        backgroundUrl = args.backgroundUrl ?: ""

        // Load background preview bằng Glide (chỉ để preview)
        if (backgroundUrl.isNotBlank()) {
            Glide.with(this)
                .load(backgroundUrl)
                .placeholder(R.color.white)
                .error(R.color.white)
                .into(binding.imageBackground)
        } else {
            binding.imageBackground.setImageResource(R.color.white)
        }

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
        binding.radioGroupFormat.check(R.id.radioMp4)
    }

    private fun setupBackgroundSelector() {
        binding.imageBackground.setOnClickListener {
            val action = ExportFragmentDirections.actionExportFragmentToBackgroundSelectionFragment(args.bitmapPathList)
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

            val bitmapList = bitmapPathList.mapNotNull { path ->
                val file = File(path)
                if (file.exists()) {
                    BitmapFactory.decodeFile(path)
                } else null
            }

            if (bitmapList.isEmpty()) {
                Toast.makeText(requireContext(), "No valid frames found", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Gọi ExportUtils, truyền backgroundUrl (link ảnh) để tải ảnh nền
            when (selectedFormat) {
                "mp4" -> ExportUtils.exportToMp4(requireContext(), bitmapList, frameRate, projectName, backgroundUrl)
                "gif" -> ExportUtils.exportToGif(requireContext(), bitmapList, frameRate, projectName, backgroundUrl)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
