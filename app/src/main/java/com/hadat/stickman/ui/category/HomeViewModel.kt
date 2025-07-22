<<<<<<< HEAD
package com.hadat.stickman.ui.category

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel

class HomeViewModel ( application : Application): AndroidViewModel(application) {

}
=======
package com.hadat.stickman.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hadat.stickman.ui.model.ItemModel

class HomeViewModel : ViewModel() {

    private val _categories = listOf(
        "Hot", "Funny", "Action", "Romance", "Horror", "Adventure", "Sport", "History", "Sci-fi", "Kids"
    )
    val categories: List<String> = _categories

    private val allItems = listOf(
        ItemModel("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQV94bbI8Jq9fkf7Vp74LnCvkFVKeoW7ODHKmDX0idkNRD7Vs-X9ovMVcug02A1kOMCCpw&usqp=CAU", "Hot Story 1", "Easy", "Hot", 7),
        ItemModel("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQV94bbI8Jq9fkf7Vp74LnCvkFVKeoW7ODHKmDX0idkNRD7Vs-X9ovMVcug02A1kOMCCpw&usqp=CAU", "Funny Story 1", "Easy", "Funny", 2),
        ItemModel("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQV94bbI8Jq9fkf7Vp74LnCvkFVKeoW7ODHKmDX0idkNRD7Vs-X9ovMVcug02A1kOMCCpw&usqp=CAU", "Action Story 1", "Easy", "Action", 3),
        ItemModel("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQV94bbI8Jq9fkf7Vp74LnCvkFVKeoW7ODHKmDX0idkNRD7Vs-X9ovMVcug02A1kOMCCpw&usqp=CAU", "Romance Story 1", "Easy", "Romance", 1),
        ItemModel("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQV94bbI8Jq9fkf7Vp74LnCvkFVKeoW7ODHKmDX0idkNRD7Vs-X9ovMVcug02A1kOMCCpw&usqp=CAU", "Horror Story 1", "Easy", "Horror", 2),
        ItemModel("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQV94bbI8Jq9fkf7Vp74LnCvkFVKeoW7ODHKmDX0idkNRD7Vs-X9ovMVcug02A1kOMCCpw&usqp=CAU", "Adventure Story 1", "Easy", "Adventure", 3),
        ItemModel("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQV94bbI8Jq9fkf7Vp74LnCvkFVKeoW7ODHKmDX0idkNRD7Vs-X9ovMVcug02A1kOMCCpw&usqp=CAU", "Sport Story 1", "Easy", "Sport", 1),
        ItemModel("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQV94bbI8Jq9fkf7Vp74LnCvkFVKeoW7ODHKmDX0idkNRD7Vs-X9ovMVcug02A1kOMCCpw&usqp=CAU", "History Story 1", "Easy", "History", 2),
        ItemModel("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQV94bbI8Jq9fkf7Vp74LnCvkFVKeoW7ODHKmDX0idkNRD7Vs-X9ovMVcug02A1kOMCCpw&usqp=CAU", "Sci-fi Story 1", "Easy", "Sci-fi", 3),
        ItemModel("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQV94bbI8Jq9fkf7Vp74LnCvkFVKeoW7ODHKmDX0idkNRD7Vs-X9ovMVcug02A1kOMCCpw&usqp=CAU", "Kids Story 1", "Easy", "Kids", 1)
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
>>>>>>> 6d571fd (feat : add ui)
