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

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> = _toastMessage

    private val _currentDrawingId = MutableLiveData<Int>()
    val currentDrawingId: LiveData<Int> = _currentDrawingId

    private val _drawingList = MutableLiveData<List<DrawingState>>()
    val drawingList: LiveData<List<DrawingState>> = _drawingList

    private var drawingView: DrawingView? = null
    private var backgroundImageView: ImageView? = null
    private val drawings = mutableListOf<DrawingState>()

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
        } ?: run {
            _toastMessage.value = "DrawingView chưa được khởi tạo"
        }
    }

    fun switchDrawing(drawingId: Int) {
        if (_currentDrawingId.value != drawingId) {
            saveCurrentDrawingState(_currentDrawingId.value ?: 0)
            _currentDrawingId.value = drawingId
            loadDrawing(drawingId)
            _toastMessage.value = "Đã chuyển sang bản vẽ ID: $drawingId"
        }
    }

    private fun saveCurrentDrawingState(drawingId: Int) {
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

    fun getDrawingList(): List<DrawingState> = drawings.toList()

    fun setMode(newMode: DrawingView.Mode, drawingId: Int) {
        if (_currentDrawingId.value == drawingId) {
            _mode.value = newMode
            drawingView?.setMode(newMode)
            when (newMode) {
                DrawingView.Mode.ERASE -> _toastMessage.value = "Chọn chế độ tẩy"
                DrawingView.Mode.FILL -> _toastMessage.value = "Chạm để đổ màu đã chọn"
                DrawingView.Mode.RECTANGLE -> _toastMessage.value = "Chọn chế độ vẽ hình chữ nhật"
                DrawingView.Mode.CIRCLE -> _toastMessage.value = "Chọn chế độ vẽ hình tròn"
                else -> _toastMessage.value = "Chọn chế độ vẽ"
            }
            saveCurrentDrawingState(drawingId)
            drawingView?.invalidate()
        }
    }

    fun setColor(newColor: Int, drawingId: Int) {
        if (_currentDrawingId.value == drawingId) {
            _color.value = newColor
            drawingView?.setColor(newColor)
            _toastMessage.value = "Đã chọn màu"
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
                    _toastMessage.value = "Kích thước tẩy: $validSize"
                }
                else -> {
                    _strokeSize.value = validSize.toFloat()
                    drawingView?.setStrokeWidth(validSize.toFloat())
                    _toastMessage.value = "Kích thước nét vẽ: $validSize"
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
            _toastMessage.value = "Độ trong suốt: $progress%"
            saveCurrentDrawingState(drawingId)
            drawingView?.invalidate()
        }
    }

    fun undo(drawingId: Int) {
        if (_currentDrawingId.value == drawingId) {
            drawingView?.undo()
            _toastMessage.value = "Hoàn tác"
            saveCurrentDrawingState(drawingId)
            drawingView?.invalidate()
        }
    }

    fun redo(drawingId: Int) {
        if (_currentDrawingId.value == drawingId) {
            drawingView?.redo()
            _toastMessage.value = "Làm lại"
            saveCurrentDrawingState(drawingId)
            drawingView?.invalidate()
        }
    }

    fun clearDrawing(drawingId: Int) {
        if (_currentDrawingId.value == drawingId) {
            drawingView?.clearDrawing()
            _toastMessage.value = "Xóa toàn bộ canvas"
            saveCurrentDrawingState(drawingId)
            drawingView?.invalidate()
        }
    }

    fun saveDrawing(drawingId: Int) {
        val drawingBitmap = drawingView?.getBitmap() ?: run {
            _toastMessage.value = "Không thể lưu bản vẽ"
            return
        }

        val backgroundDrawable = backgroundImageView?.drawable
        val backgroundBitmap = if (backgroundDrawable is BitmapDrawable) {
            backgroundDrawable.bitmap
        } else {
            Bitmap.createBitmap(drawingBitmap.width, drawingBitmap.height, Bitmap.Config.ARGB_8888).apply {
                eraseColor(Color.WHITE)
            }
        }

        val finalBitmap = Bitmap.createBitmap(drawingBitmap.width, drawingBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(finalBitmap)
        canvas.drawBitmap(backgroundBitmap, 0f, 0f, null)
        canvas.drawBitmap(drawingBitmap, 0f, 0f, null)

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Drawing_${drawingId}_$timeStamp.png"

        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/StickmanDrawings")
            }

            val resolver = getApplication<Application>().contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    _toastMessage.value = "Bản vẽ đã được lưu vào thư viện"
                }
            } ?: run {
                _toastMessage.value = "Không thể lưu bản vẽ"
            }
        } catch (e: Exception) {
            _toastMessage.value = "Lỗi khi lưu bản vẽ: ${e.message}"
        } finally {
            finalBitmap.recycle()
        }
        saveCurrentDrawingState(drawingId)
    }

    fun getSizeForMode(): Pair<Int, Int> {
        return when (_mode.value) {
            DrawingView.Mode.ERASE -> Pair(_eraserSize.value!!.toInt(), 100)
            else -> Pair(_strokeSize.value!!.toInt(), 50)
        }
    }
}