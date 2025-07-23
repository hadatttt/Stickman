package com.hadat.stickman.ui.category

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hadat.stickman.ui.model.ItemModel

class HomeViewModel : ViewModel() {

    private val _categories = listOf(
        "Hot", "Funny", "Action", "Romance", "Horror", "Adventure", "Sport", "History", "Sci-fi", "Kids"
    )
    val categories: List<String> = _categories

    val allItems = listOf(
        ItemModel(
            imageUrl = listOf(
                "https://cdn-icons-png.flaticon.com/512/1216/1216733.png",
                "https://cdn-icons-png.flaticon.com/512/1216/1216729.png"
            ),
            title = "Stickman Battle",
            level = "Easy",
            category = "Hot",
            frame = 2
        ),
        ItemModel(
            imageUrl = listOf(
                "https://cdn-icons-png.flaticon.com/512/1216/1216732.png"
            ),
            title = "Stickman Fight",
            level = "Medium",
            category = "New",
            frame = 1
        ),
        ItemModel(
            imageUrl = listOf(
                "https://cdn-icons-png.flaticon.com/512/1216/1216731.png",
                "https://cdn-icons-png.flaticon.com/512/1216/1216728.png",
                "https://cdn-icons-png.flaticon.com/512/1216/1216730.png"
            ),
            title = "Stickman Adventure",
            level = "Hard",
            category = "Hot",
            frame = 3
        ),
        ItemModel(
            imageUrl = listOf(
                "https://cdn-icons-png.flaticon.com/512/1216/1216726.png"
            ),
            title = "Stickman Parkour",
            level = "Easy",
            category = "Fun",
            frame = 1
        ),
        ItemModel(
            imageUrl = listOf(
                "https://cdn-icons-png.flaticon.com/512/1216/1216725.png",
                "https://cdn-icons-png.flaticon.com/512/1216/1216724.png"
            ),
            title = "Stickman Climb",
            level = "Medium",
            category = "Challenging",
            frame = 2
        ),
        ItemModel(
            imageUrl = listOf(
                "https://cdn-icons-png.flaticon.com/512/1216/1216723.png",
                "https://cdn-icons-png.flaticon.com/512/1216/1216722.png",
                "https://cdn-icons-png.flaticon.com/512/1216/1216721.png",
                "https://cdn-icons-png.flaticon.com/512/1216/1216720.png"
            ),
            title = "Stickman Swing",
            level = "Hard",
            category = "Popular",
            frame = 4
        ),
        ItemModel(
            imageUrl = listOf(
                "https://cdn-icons-png.flaticon.com/512/1216/1216719.png"
            ),
            title = "Stickman Puzzle",
            level = "Easy",
            category = "Logic",
            frame = 1
        ),
        ItemModel(
            imageUrl = listOf(
                "https://cdn-icons-png.flaticon.com/512/1216/1216718.png",
                "https://cdn-icons-png.flaticon.com/512/1216/1216717.png"
            ),
            title = "Stickman Ninja",
            level = "Medium",
            category = "Stealth",
            frame = 2
        ),
        ItemModel(
            imageUrl = listOf(
                "https://cdn-icons-png.flaticon.com/512/1216/1216716.png",
                "https://cdn-icons-png.flaticon.com/512/1216/1216715.png",
                "https://cdn-icons-png.flaticon.com/512/1216/1216714.png"
            ),
            title = "Stickman Quest",
            level = "Hard",
            category = "Adventure",
            frame = 3
        ),
        ItemModel(
            imageUrl = listOf(
                "https://cdn-icons-png.flaticon.com/512/1216/1216713.png"
            ),
            title = "Stickman Escape",
            level = "Easy",
            category = "Fun",
            frame = 1
        )
    )

    private val _items = MutableLiveData<List<ItemModel>>()
    val items: LiveData<List<ItemModel>> get() = _items

    init {
        _items.value = allItems
    }

    fun filterItems(category: String) {
        _items.value = if (category == "All") allItems else allItems.filter { it.category == category }
    }
}

