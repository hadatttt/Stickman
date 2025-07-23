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

    private var selectedPosition: Int = 0

    inner class FrameViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val previewImage: ImageView = itemView.findViewById(R.id.imgFrame)
        val txtFrameId: TextView = itemView.findViewById(R.id.txtFrameID)

        fun bind(frame: FrameModel, isSelected: Boolean) {
            txtFrameId.text = "${frame.id}"
            frame.previewBitmap?.let {
                if (!it.isRecycled) {
                    previewImage.setImageBitmap(it)
                } else {
                    previewImage.setImageDrawable(null)
                }
            } ?: run {
                previewImage.setImageDrawable(null)
            }
            itemView.isSelected = isSelected
            itemView.alpha = if (isSelected) 1f else 0.5f
        }

        init {
            itemView.setOnClickListener {
                val frame = frameList[adapterPosition]
                val previousPosition = selectedPosition
                selectedPosition = adapterPosition
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)
                onItemClick(frame.id)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FrameViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_frame, parent, false)
        return FrameViewHolder(view)
    }

    override fun onBindViewHolder(holder: FrameViewHolder, position: Int) {
        holder.bind(frameList[position], position == selectedPosition)
    }

    override fun getItemCount(): Int = frameList.size

    fun updateSelectedPosition(drawingId: Int) {
        val previousPosition = selectedPosition
        selectedPosition = frameList.indexOfFirst { it.id == drawingId }
        if (selectedPosition != -1) {
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)
        }
    }
}