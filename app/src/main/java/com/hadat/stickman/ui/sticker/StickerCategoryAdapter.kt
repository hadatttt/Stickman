package com.hadat.stickman.ui.sticker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hadat.stickman.databinding.ItemStickerCategoryBinding
import com.hadat.stickman.model.StickerCategoryModel

class StickerCategoryAdapter(
    private val categories: List<StickerCategoryModel>,
    private val onCategoryClick: (Int) -> Unit
) : RecyclerView.Adapter<StickerCategoryAdapter.CategoryViewHolder>() {

    private var selectedPosition = -1

    inner class CategoryViewHolder(private val binding: ItemStickerCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(category: StickerCategoryModel, isSelected: Boolean) {
            Glide.with(binding.root.context)
                .load(category.imageUrl)
                .into(binding.categoryImageView)

            binding.root.alpha = if (isSelected) 1f else 0.5f

            binding.root.setOnClickListener {
                val previous = selectedPosition
                selectedPosition = adapterPosition
                notifyItemChanged(previous)
                notifyItemChanged(selectedPosition)
                onCategoryClick(category.id)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemStickerCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.bind(category, position == selectedPosition)
    }

    override fun getItemCount(): Int = categories.size
}
