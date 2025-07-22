package com.hadat.stickman.ui.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ItemModel(
    val imageUrl: String,
    val title: String,
    val level: String,
<<<<<<< HEAD
    val category: String
=======
    val category: String,
    val frame : Int
>>>>>>> 6d571fd (feat : add ui)
) : Parcelable
