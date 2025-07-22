package com.hadat.stickman.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
<<<<<<< HEAD
import com.hadat.stickman.databinding.FragmentHomeBinding

/**
 * A simple [androidx.fragment.app.Fragment] subclass as the second destination in the navigation.
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root

=======
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.hadat.stickman.databinding.FragmentHomeBinding
import com.hadat.stickman.ui.category.CategoryAdapter
import com.hadat.stickman.ui.category.ItemAdapter
import com.hadat.stickman.ui.model.ItemModel

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var itemAdapter: ItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
>>>>>>> 6d571fd (feat : add ui)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

<<<<<<< HEAD
=======
        // Setup Category RecyclerView
        categoryAdapter = CategoryAdapter(viewModel.categories) { selectedCategory ->
            viewModel.filterItems(selectedCategory)
        }
        binding.recyclerCategory.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerCategory.adapter = categoryAdapter

        // Setup Item RecyclerView
        itemAdapter = ItemAdapter(mutableListOf()) { item ->
            val action = HomeFragmentDirections.actionHomeFragmentToDrawingFragment(item)
            findNavController().navigate(action)
        }
        binding.recyclerViewTemplates.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerViewTemplates.adapter = itemAdapter

        // Observe LiveData items from ViewModel
        viewModel.items.observe(viewLifecycleOwner, Observer { items ->
            itemAdapter.updateData(items)
        })
>>>>>>> 6d571fd (feat : add ui)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
<<<<<<< HEAD
}
=======
}
>>>>>>> 6d571fd (feat : add ui)
