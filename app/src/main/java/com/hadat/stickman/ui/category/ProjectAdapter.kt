package com.hadat.stickman.ui.home

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hadat.stickman.R
import com.hadat.stickman.ui.model.ProjectModel

class ProjectAdapter(
    private val context: Context,
    private val projects: List<ProjectModel>,
    private val onItemClick: (String, String) -> Unit,           // click để xem
    private val onItemLongClick: (ProjectModel) -> Unit          // long click để xóa
) : RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder>() {

    inner class ProjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val projectImage: ImageView = itemView.findViewById(R.id.projectImage)
        val projectName: TextView = itemView.findViewById(R.id.projectName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_project, parent, false)
        return ProjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        val project = projects[position]
        holder.projectName.text = project.name

            val thumbnailRequest = Glide.with(context)
                .load(project.videoUrl)
                .override(100, 100)

            Glide.with(context)
                .load(project.videoUrl)
                .thumbnail(thumbnailRequest)
                .placeholder(R.color.white)
                .error(R.color.white)
                .into(holder.projectImage)

        // Click để xem
        holder.itemView.setOnClickListener {
            project.videoUrl?.let { url ->
                onItemClick(project.name, url)
            }
        }

        // Long click để xóa
        holder.itemView.setOnLongClickListener {
            onItemLongClick(project)
            true
        }
    }

    override fun getItemCount(): Int = projects.size
}
