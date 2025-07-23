
package com.hadat.stickman.ui.category

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Environment
import android.provider.MediaStore
import android.widget.ImageView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.hadat.stickman.ui.home.DrawingView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DrawingViewModel(application: Application) : AndroidViewModel(application) {

    private val _mode = MutableLiveData(DrawingView.Mode.DRAW)
    val mode: LiveData<DrawingView.Mode> = _mode

    private val _strokeSize = MutableLiveData(10f)
    val strokeSize: LiveData<Float> = _strokeSize

    private val _eraserSize = MutableLiveData(50f)
    val eraserSize: LiveData<Float> = _eraserSize

    private val _color = MutableLiveData(Color.BLACK)
    val color: LiveData<Int> = _color

    private val _opacity = MutableLiveData(100)
    val opacity: LiveData<Int> = _opacity

    private val _currentDrawingId = MutableLiveData<Int>()
    val currentDrawingId: LiveData<Int> = _currentDrawingId

    private val _drawingList = MutableLiveData<List<DrawingState>>()
    val drawingList: LiveData<List<DrawingState>> = _drawingList

    private var drawingView: DrawingView? = null
    private var backgroundImageView: ImageView? = null
    private val drawings = mutableListOf<DrawingState>()
    private var imageUrls: List<String> = emptyList()

    data class DrawingState(
        val id: Int,
        val bitmap: Bitmap?,
        val commands: List<DrawingView.Command>,
        val fillShapes: List<Pair<android.graphics.Path, android.graphics.Paint>>,
        val strokeColor: Int,
        val strokeWidth: Float,
        val eraserSize: Float,
        val brushAlpha: Int,
        val mode: DrawingView.Mode,
        val scaleFactor: Float,
        val translateX: Float,
        val translateY: Float
    )

    fun setImageUrls(urls: List<String>) {
        imageUrls = urls
    }

    fun getImageUrlForDrawing(drawingId: Int): String? {
        val index = drawingId - 1
        return if (index in imageUrls.indices) imageUrls[index] else null
    }

    fun setDrawingView(view: DrawingView, backgroundView: ImageView, drawingId: Int = 1) {
        drawingView = view
        backgroundImageView = backgroundView
        _currentDrawingId.value = drawingId
        loadDrawing(drawingId)
    }

    private fun loadDrawing(drawingId: Int) {
        drawingView?.let { view ->
            val existingDrawing = drawings.find { it.id == drawingId }
            if (existingDrawing != null) {
                view.setDrawingState(existingDrawing)
                view.invalidate()
            } else {
                view.setMode(_mode.value!!)
                view.setColor(_color.value!!)
                view.setStrokeWidth(_strokeSize.value!!)
                view.setEraserSize(_eraserSize.value!!)
                view.setBrushAlpha((_opacity.value!! * 2.55).toInt())
                view.clearDrawing()
                saveCurrentDrawingState(drawingId)
                view.invalidate()
            }
        }
    }

    fun switchDrawing(drawingId: Int) {
        if (_currentDrawingId.value != drawingId) {
            saveCurrentDrawingState(_currentDrawingId.value ?: 0)
            _currentDrawingId.value = drawingId
            loadDrawing(drawingId)
        }
    }

    fun saveCurrentDrawingState(drawingId: Int) {
        drawingView?.let { view ->
            val existingIndex = drawings.indexOfFirst { it.id == drawingId }
            val newState = DrawingState(
                id = drawingId,
                bitmap = view.getBitmap(),
                commands = view.getCommandHistory(),
                fillShapes = view.getFillShapes(),
                strokeColor = _color.value!!,
                strokeWidth = _strokeSize.value!!,
                eraserSize = _eraserSize.value!!,
                brushAlpha = (_opacity.value!! * 2.55).toInt(),
                mode = _mode.value!!,
                scaleFactor = view.getScaleFactor(),
                translateX = view.getTranslateX(),
                translateY = view.getTranslateY()
            )
            if (existingIndex >= 0) {
                drawings[existingIndex] = newState
            } else {
                drawings.add(newState)
            }
            _drawingList.postValue(drawings.toList())
        }
    }

    fun addNewDrawing(frameId: Int) {
        // Check if the drawing already exists to avoid duplicates
        if (drawings.any { it.id == frameId }) return

        // Create a new DrawingState with default values
        val newState = DrawingState(
            id = frameId,
            bitmap = null, // New frame starts with no bitmap
            commands = emptyList(),
            fillShapes = emptyList(),
            strokeColor = _color.value ?: Color.BLACK,
            strokeWidth = _strokeSize.value ?: 10f,
            eraserSize = _eraserSize.value ?: 50f,
            brushAlpha = (_opacity.value!! * 2.55).toInt(),
            mode = _mode.value ?: DrawingView.Mode.DRAW,
            scaleFactor = 1f,
            translateX = 0f,
            translateY = 0f
        )
        drawings.add(newState)
        _drawingList.postValue(drawings.toList())
    }

    fun getDrawingList(): List<DrawingState> = drawings.toList()

    fun setMode(newMode: DrawingView.Mode, drawingId: Int) {
        if (_currentDrawingId.value == drawingId) {
            _mode.value = newMode
            drawingView?.setMode(newMode)
            saveCurrentDrawingState(drawingId)
            drawingView?.invalidate()
        }
    }

    fun setColor(newColor: Int, drawingId: Int) {
        if (_currentDrawingId.value == drawingId) {
            _color.value = newColor
            drawingView?.setColor(newColor)
            saveCurrentDrawingState(drawingId)
            drawingView?.invalidate()
        }
    }

    fun setSize(size: Int, drawingId: Int) {
        if (_currentDrawingId.value == drawingId) {
            val validSize = if (size < 1) 1 else size
            when (_mode.value) {
                DrawingView.Mode.ERASE -> {
                    _eraserSize.value = validSize.toFloat()
                    drawingView?.setEraserSize(validSize.toFloat())
                }
                else -> {
                    _strokeSize.value = validSize.toFloat()
                    drawingView?.setStrokeWidth(validSize.toFloat())
                }
            }
            saveCurrentDrawingState(drawingId)
            drawingView?.invalidate()
        }
    }

    fun setOpacity(progress: Int, drawingId: Int) {
        if (_currentDrawingId.value == drawingId) {
            _opacity.value = progress
            drawingView?.setBrushAlpha((progress * 2.55).toInt())
            saveCurrentDrawingState(drawingId)
            drawingView?.invalidate()
        }
    }

    fun undo(drawingId: Int) {
        if (_currentDrawingId.value == drawingId) {
            drawingView?.undo()
            saveCurrentDrawingState(drawingId)
            drawingView?.invalidate()
        }
    }

    fun redo(drawingId: Int) {
        if (_currentDrawingId.value == drawingId) {
            drawingView?.redo()
            saveCurrentDrawingState(drawingId)
            drawingView?.invalidate()
        }
    }

    fun clearDrawing(drawingId: Int) {
        if (_currentDrawingId.value == drawingId) {
            drawingView?.clearDrawing()
            saveCurrentDrawingState(drawingId)
            drawingView?.invalidate()
        }
    }

    fun getSizeForMode(): Pair<Int, Int> {
        return when (_mode.value) {
            DrawingView.Mode.ERASE -> Pair(_eraserSize.value!!.toInt(), 100)
            else -> Pair(_strokeSize.value!!.toInt(), 50)
        }
    }
}
