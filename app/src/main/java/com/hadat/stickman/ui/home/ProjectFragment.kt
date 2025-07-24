package com.hadat.stickman.ui.home

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.hadat.stickman.databinding.FragmentProjectBinding
import com.hadat.stickman.ui.database.AppDatabase
import com.hadat.stickman.ui.model.ProjectModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProjectFragment : Fragment() {

    private var _binding: FragmentProjectBinding? = null
    private val binding get() = _binding!!

    private lateinit var projectDao: com.hadat.stickman.ui.database.ProjectDao
    private lateinit var projectAdapter: ProjectAdapter
    private var projectList: List<ProjectModel> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = Room.databaseBuilder(
            requireContext().applicationContext,
            AppDatabase::class.java,
            "stickman_database"
        ).build()
        projectDao = db.projectDao()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProjectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recycleViewProject.layoutManager = LinearLayoutManager(requireContext())

        fetchAllProjects()
    }

    private fun fetchAllProjects() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val projectEntities = projectDao.getAllProjects()

                projectList = projectEntities.map { entity ->
                    ProjectModel(
                        id = entity.id,
                        name = entity.name,
                        videoUrl = entity.videoUrl
                    )
                }

                withContext(Dispatchers.Main) {
                    projectAdapter = ProjectAdapter(
                        requireContext(),
                        projectList,
                        onItemClick = { name, videoUrl ->
                            val action = ProjectFragmentDirections.actionProjectFragmentToPreviewVideoFragment(name, videoUrl)
                            findNavController().navigate(action)
                        },
                        onItemLongClick = { project ->
                            showDeleteConfirmationDialog(project)
                        }
                    )
                    binding.recycleViewProject.adapter = projectAdapter

                    Log.d("ProjectFragment", "Danh sách dự án: $projectList")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("ProjectFragment", "Lỗi khi lấy danh sách dự án", e)
                }
            }
        }
    }

    private fun showDeleteConfirmationDialog(project: ProjectModel) {
        AlertDialog.Builder(requireContext())
            .setTitle("Xóa dự án")
            .setMessage("Bạn có chắc muốn xóa dự án \"${project.name}\" không?")
            .setPositiveButton("Xóa") { dialog, _ ->
                dialog.dismiss()
                deleteProject(project)
            }
            .setNegativeButton("Hủy") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun deleteProject(project: ProjectModel) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                projectDao.deleteProjectById(project.id) // cần viết hàm deleteProjectById trong DAO
                fetchAllProjects() // tải lại danh sách sau khi xóa
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("ProjectFragment", "Lỗi khi xóa dự án", e)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
