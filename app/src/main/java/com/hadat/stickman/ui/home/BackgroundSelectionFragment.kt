package com.hadat.stickman.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hadat.stickman.R
import com.hadat.stickman.ui.category.BackgroundAdapter
import kotlin.getValue

class BackgroundSelectionFragment : Fragment() {
    private val args: BackgroundSelectionFragmentArgs by navArgs()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BackgroundAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_background_selection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.recyclerBackgrounds)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)

        val backgroundUrls = listOf(
            "https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=400&q=80",
            "https://images.unsplash.com/photo-1494526585095-c41746248156?auto=format&fit=crop&w=400&q=80",
            "https://images.unsplash.com/photo-1500534623283-312aade485b7?auto=format&fit=crop&w=400&q=80",
            "https://images.unsplash.com/photo-1470770841072-f978cf4d019e?auto=format&fit=crop&w=400&q=80",
            "https://images.unsplash.com/photo-1491553895911-0055eca6402d?auto=format&fit=crop&w=400&q=80",
            "https://images.unsplash.com/photo-1519125323398-675f0ddb6308?auto=format&fit=crop&w=400&q=80"
        )

        adapter = BackgroundAdapter(backgroundUrls) { selectedUrl ->
            val action = BackgroundSelectionFragmentDirections
                .actionBackgroundSelectionFragmentToExportFragment(args.bitmapPathList,selectedUrl)
            findNavController().navigate(action)
        }

        recyclerView.adapter = adapter
    }
}
