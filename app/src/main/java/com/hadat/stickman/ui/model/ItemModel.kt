package com.hadat.stickman.ui.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ItemModel(
    val imageUrl: String,
    val title: String,
    val level: String,
    val category: String
) : Parcelable
