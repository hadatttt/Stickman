package com.hadat.stickman.ui.category

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.hadat.stickman.R

class CategoryAdapter(
    private val categories: List<String>,
    private val onCategoryClick: (String) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private var selectedPosition = 0

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtCategory: TextView = itemView.findViewById(R.id.txtCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.txtCategory.text = category

        val context = holder.itemView.context
        if (position == selectedPosition) {
            holder.txtCategory.setTextColor(ContextCompat.getColor(context, R.color.white))
            // holder.txtCategory.setBackgroundResource(R.drawable.bg_category_selected)
        } else {
            holder.txtCategory.setTextColor(ContextCompat.getColor(context, R.color.black))
            // holder.txtCategory.setBackgroundResource(R.drawable.bg_category_unselected)
        }

        holder.itemView.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                val prev = selectedPosition
                selectedPosition = currentPosition
                notifyItemChanged(prev)
                notifyItemChanged(currentPosition)
                onCategoryClick(categories[currentPosition])
            }
        }
    }


    override fun getItemCount(): Int = categories.size
}
