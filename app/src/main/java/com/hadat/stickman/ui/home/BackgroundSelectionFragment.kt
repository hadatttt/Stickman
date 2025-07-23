package com.hadat.stickman.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hadat.stickman.R
import com.hadat.stickman.ui.category.BackgroundAdapter

class BackgroundSelectionFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BackgroundAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_background_selection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.recyclerBackgrounds)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)

        val backgroundList = listOf(
            R.drawable.bg1,
            R.drawable.bg2,
            R.drawable.bg2,
            R.drawable.bg1,
            R.drawable.bg2,
            R.drawable.bg1
        )

        adapter = BackgroundAdapter(backgroundList) { selectedResId ->
            // Truyền selectedResId về Fragment trước đó hoặc ViewModel
            parentFragmentManager.popBackStack()
            (activity as? OnBackgroundSelectedListener)?.onBackgroundSelected(selectedResId)
        }

        recyclerView.adapter = adapter
    }

    interface OnBackgroundSelectedListener {
        fun onBackgroundSelected(backgroundResId: Int)
    }
}
