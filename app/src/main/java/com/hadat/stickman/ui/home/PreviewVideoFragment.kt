package com.hadat.stickman.ui.home

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.hadat.stickman.databinding.FragmentPreviewVideoBinding

class PreviewVideoFragment : Fragment() {

    private var _binding: FragmentPreviewVideoBinding? = null
    private val binding get() = _binding!!

    private val args: PreviewVideoFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPreviewVideoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set tên dự án
        binding.projectNameTextView.text = args.name

        // Lấy URI video
        val videoUri = Uri.parse(args.videoUrl)

        // Set video và controller
        val mediaController = MediaController(requireContext())
        mediaController.setAnchorView(binding.videoView)

        binding.videoView.apply {
            setVideoURI(videoUri)
            setMediaController(mediaController)
            requestFocus()
            start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
