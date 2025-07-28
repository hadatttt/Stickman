package com.hadat.stickman.ui.category

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hadat.stickman.R
import com.hadat.stickman.databinding.ItemAddFrameBinding
import com.hadat.stickman.ui.model.FrameModel

class FrameAdapter(
    private val frameList: List<FrameModel>,
    private val onItemClick: (Int) -> Unit,
    private val onAddFrameClick: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_FRAME = 0
        private const val VIEW_TYPE_ADD_FRAME = 1
    }

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

    inner class AddFrameViewHolder(binding: ItemAddFrameBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                onAddFrameClick()
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == frameList.size) VIEW_TYPE_ADD_FRAME else VIEW_TYPE_FRAME
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_FRAME -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_frame, parent, false)
                FrameViewHolder(view)
            }
            VIEW_TYPE_ADD_FRAME -> {
                val binding = ItemAddFrameBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                AddFrameViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Loại view không hợp lệ")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is FrameViewHolder) {
            holder.bind(frameList[position], position == selectedPosition)
        }
        // Không cần bind cho AddFrameViewHolder vì nó chỉ là biểu tượng tĩnh
    }

    override fun getItemCount(): Int = frameList.size + 1 // Thêm 1 cho mục "Thêm Frame"

    fun updateSelectedPosition(drawingId: Int) {
        val previousPosition = selectedPosition
        selectedPosition = frameList.indexOfFirst { it.id == drawingId }
        if (selectedPosition != -1) {
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)
        }
    }
}