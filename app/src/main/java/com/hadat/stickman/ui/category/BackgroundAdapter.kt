package com.hadat.stickman.ui.category

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hadat.stickman.R

class BackgroundAdapter(
    private val items: List<String>,  // đổi sang List<String> chứa URL ảnh
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<BackgroundAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imagePreview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_background, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val url = items[position]
        Glide.with(holder.imageView.context)
            .load(url)
            .centerCrop().into(holder.imageView)

        holder.itemView.setOnClickListener { onClick(url) }
    }
}
