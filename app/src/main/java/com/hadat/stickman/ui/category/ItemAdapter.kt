package com.hadat.stickman.ui.category

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hadat.stickman.R
import com.hadat.stickman.ui.model.ItemModel

class ItemAdapter(
    private val itemList: MutableList<ItemModel>,
    private val onItemClick: (ItemModel) -> Unit
) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgThumb: ImageView = itemView.findViewById(R.id.imgThumb)
        val txtTitle: TextView = itemView.findViewById(R.id.txtTitle)
        val txtLevel: TextView = itemView.findViewById(R.id.txtLevel)
        val txtFrame: TextView = itemView.findViewById(R.id.txtFrame)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_template, parent, false)  // đổi tên layout cho đúng
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = itemList[position]
        Glide.with(holder.itemView.context)
            .load(item.imageUrl)
            .centerCrop()
            .override(100, 100)
            .into(holder.imgThumb)
        holder.txtFrame.text="Frame: ${item.frame}"
        holder.txtTitle.text = item.title
        holder.txtLevel.text = "Level: ${item.level}"
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = itemList.size

    fun updateData(newList: List<ItemModel>) {
        itemList.clear()
        itemList.addAll(newList)
        notifyDataSetChanged()
    }
}
