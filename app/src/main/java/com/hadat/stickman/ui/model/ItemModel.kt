package com.hadat.stickman.ui.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ItemModel(
    val imageUrl: List<String>,
    val title: String,
    val level: String,
    val category: String,
    val frame : Int
) : Parcelable
