package com.hadat.stickman.ui.home

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.hadat.stickman.R
import com.hadat.stickman.databinding.FragmentExportBinding
import com.hadat.stickman.ui.utils.ExportUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ExportFragment : Fragment() {

    private var _binding: FragmentExportBinding? = null
    private val binding get() = _binding!!

    private val args: ExportFragmentArgs by navArgs()

    private val backgroundUrl: String by lazy { args.backgroundUrl ?: "" }
    private val bitmapPathList: List<String> by lazy { args.bitmapPathList?.toList() ?: emptyList() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ExportUtils.initialize(requireContext()) // Initialize projectDao

        // Handle back press to return to HomeFragment
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val action = ExportFragmentDirections.actionExportFragmentToHomeFragment()
                findNavController().navigate(action)
            }
        })

        // Display background image
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
        setupBackgroundSelector()
        setupCreateButton()
    }

    private fun setupSpinners() {
        val aspectRatios = listOf("1:1", "16:9", "4:3", "3:4")
        val frameRates = (1..30).map { "$it fps" }

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
                .replace(" fps", "").toIntOrNull() ?: 1
            val aspectRatio = binding.spinnerAspectRatio.selectedItem.toString()

            if (bitmapPathList.isEmpty()) {
                Toast.makeText(requireContext(), "No frames to export", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val bitmapList = withContext(Dispatchers.IO) {
                    bitmapPathList.mapNotNull { path ->
                        val file = File(path)
                        if (file.exists()) BitmapFactory.decodeFile(path) else null
                    }
                }

                if (bitmapList.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "No valid frames found", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // Export to mp4 with aspect ratio
                ExportUtils.exportToMp4(requireContext(), bitmapList, frameRate, projectName, backgroundUrl, aspectRatio)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}