package com.hadat.stickman.ui.category

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
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

    private var drawingView: DrawingView? = null
    private var backgroundImageView: ImageView? = null

    fun setDrawingView(view: DrawingView, backgroundView: ImageView) {
        drawingView = view
        backgroundImageView = backgroundView
        drawingView?.setMode(_mode.value!!)
        drawingView?.setColor(_color.value!!)
        drawingView?.setStrokeWidth(_strokeSize.value!!)
        drawingView?.setEraserSize(_eraserSize.value!!)
        drawingView?.setBrushAlpha((_opacity.value!! * 2.55).toInt())
    }

    fun setMode(newMode: DrawingView.Mode) {
        _mode.value = newMode
        drawingView?.setMode(newMode)
        when (newMode) {
            DrawingView.Mode.ERASE -> {
                _toastMessage.value = "Chọn chế độ tẩy"
            }
            DrawingView.Mode.FILL -> {
                _toastMessage.value = "Chạm để đổ màu đã chọn"
            }
            DrawingView.Mode.RECTANGLE -> {
                _toastMessage.value = "Chọn chế độ vẽ hình chữ nhật"
            }
            DrawingView.Mode.CIRCLE -> {
                _toastMessage.value = "Chọn chế độ vẽ hình tròn"
            }
            else -> {
                _toastMessage.value = "Chọn chế độ vẽ"
            }
        }
    }

    fun setColor(newColor: Int) {
        _color.value = newColor
        drawingView?.setColor(newColor)
        _toastMessage.value = "Đã chọn màu"
    }

    fun setSize(size: Int) {
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
    }

    fun setOpacity(progress: Int) {
        _opacity.value = progress
        drawingView?.setBrushAlpha((progress * 2.55).toInt())
        _toastMessage.value = "Độ trong suốt: $progress%"
    }

    fun undo() {
        drawingView?.undo()
        _toastMessage.value = "Hoàn tác"
    }

    fun redo() {
        drawingView?.redo()
        _toastMessage.value = "Làm lại"
    }

    fun clearDrawing() {
        drawingView?.clearDrawing()
        _toastMessage.value = "Xóa toàn bộ canvas"
    }

    fun saveDrawing() {
        val drawingBitmap = drawingView?.getBitmap() ?: run {
            _toastMessage.value = "Không thể lưu bản vẽ"
            return
        }

        // Lấy hình nền từ ImageView
        val backgroundDrawable = backgroundImageView?.drawable
        val backgroundBitmap = if (backgroundDrawable is android.graphics.drawable.BitmapDrawable) {
            backgroundDrawable.bitmap
        } else {
            // Tạo bitmap trắng mặc định nếu không có hình nền
            Bitmap.createBitmap(drawingBitmap.width, drawingBitmap.height, Bitmap.Config.ARGB_8888).apply {
                eraseColor(Color.WHITE)
            }
        }

        // Kết hợp hình nền và lớp vẽ
        val finalBitmap = Bitmap.createBitmap(drawingBitmap.width, drawingBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(finalBitmap)
        canvas.drawBitmap(backgroundBitmap, 0f, 0f, null)
        canvas.drawBitmap(drawingBitmap, 0f, 0f, null)

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Drawing_$timeStamp.png"

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
    }

    fun getSizeForMode(): Pair<Int, Int> {
        return when (_mode.value) {
            DrawingView.Mode.ERASE -> Pair(_eraserSize.value!!.toInt(), 100)
            else -> Pair(_strokeSize.value!!.toInt(), 50)
        }
    }
}