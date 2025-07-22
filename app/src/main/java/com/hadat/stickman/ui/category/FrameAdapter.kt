package com.hadat.stickman.ui.category

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hadat.stickman.R
import com.hadat.stickman.ui.model.FrameModel

class FrameAdapter(
    private val frameList: List<FrameModel>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<FrameAdapter.FrameViewHolder>() {

    inner class FrameViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val previewImage: ImageView = itemView.findViewById(R.id.imgFrame)
        val txtFrameId: TextView = itemView.findViewById(R.id.txtFrameID) // thêm dòng này

        init {
            itemView.setOnClickListener {
                onItemClick(adapterPosition)
            }
        }

        fun bind(frame: FrameModel) {
            txtFrameId.text = "${frame.id}" // hiển thị id
            frame.previewBitmap?.let { previewImage.setImageBitmap(it) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FrameViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_frame, parent, false)
        return FrameViewHolder(view)
    }

    override fun onBindViewHolder(holder: FrameViewHolder, position: Int) {
        holder.bind(frameList[position])
    }

    override fun getItemCount(): Int = frameList.size
}

