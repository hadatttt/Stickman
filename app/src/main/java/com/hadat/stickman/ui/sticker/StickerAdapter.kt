package com.hadat.stickman.ui.sticker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hadat.stickman.databinding.ItemStickerBinding
import com.hadat.stickman.model.StickerModel

class StickerAdapter(
    private var stickerList: List<StickerModel>,
    private val onClick: (StickerModel) -> Unit
) : RecyclerView.Adapter<StickerAdapter.StickerViewHolder>() {

    inner class StickerViewHolder(private val binding: ItemStickerBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(sticker: StickerModel) {
            Glide.with(binding.root.context)
                .load(sticker.imageUrl)
                .into(binding.stickerImageView)

            binding.root.setOnClickListener {
                onClick(sticker)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StickerViewHolder {
        val binding = ItemStickerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StickerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StickerViewHolder, position: Int) {
        holder.bind(stickerList[position])
    }

    override fun getItemCount(): Int = stickerList.size

    fun updateData(newList: List<StickerModel>) {
        stickerList = newList
        notifyDataSetChanged()
    }
}
