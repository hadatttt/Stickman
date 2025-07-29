package com.hadat.stickman.ui.category

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Environment
import android.provider.MediaStore
import android.widget.ImageView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bumptech.glide.Glide
import com.hadat.stickman.ui.home.DrawingView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

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

    private val _similarityPercentage = MutableLiveData<Int>()
    val similarityPercentage: LiveData<Int> = _similarityPercentage

    private var drawingView: DrawingView? = null
    private var backgroundImageView: ImageView? = null
    private val drawings = mutableListOf<DrawingState>()
    private var imageUrls: Array<String> = emptyArray()

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

    fun setImageUrls(urls: Array<String>) {
        imageUrls = urls
    }

    fun getImageUrlForDrawing(drawingId: Int): String? {
        val index = drawingId - 1
        return if (index in imageUrls.indices) imageUrls[index] else null
    }

    fun setDrawingView(view: DrawingView, backgroundView: ImageView, drawingId: Int = 1) {
        drawingView = view
        backgroundImageView = backgroundView
        setBitmapUpdateListener(view)
        _currentDrawingId.value = drawingId
        loadDrawing(drawingId)
    }

    private fun loadDrawing(drawingId: Int) {
        drawingView?.let { view ->
            val existingDrawing = drawings.find { it.id == drawingId }
            if (existingDrawing != null) {
                view.setDrawingState(existingDrawing)
                _mode.value = existingDrawing.mode
                _color.value = existingDrawing.strokeColor
                _strokeSize.value = existingDrawing.strokeWidth
                _eraserSize.value = existingDrawing.eraserSize
                _opacity.value = (existingDrawing.brushAlpha / 2.55).toInt()
                _similarityPercentage.postValue(0) // Reset khi load frame mới
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
        if (drawings.any { it.id == frameId }) return

        val newState = DrawingState(
            id = frameId,
            bitmap = null,
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
            DrawingView.Mode.COLOR_PICKER -> Pair(_strokeSize.value!!.toInt(), 50)
            else -> Pair(_strokeSize.value!!.toInt(), 50)
        }
    }

    fun setBitmapUpdateListener(drawingView: DrawingView) {
        drawingView.onBitmapUpdated = onBitmapUpdated@{ bitmap ->
            val drawingId = _currentDrawingId.value ?: return@onBitmapUpdated
            updateDrawingStateBitmap(drawingId, bitmap)
            val imageUrl = getImageUrlForDrawing(drawingId)
            if (imageUrl != null && bitmap != null) {
                compareBitmapWithUrl(imageUrl, bitmap) { similarity ->
                    _similarityPercentage.postValue(similarity)
                }
            }
        }
    }

    private fun updateDrawingStateBitmap(drawingId: Int, bitmap: Bitmap?) {
        val existingIndex = drawings.indexOfFirst { it.id == drawingId }
        if (existingIndex >= 0) {
            val existingDrawing = drawings[existingIndex]
            drawings[existingIndex] = existingDrawing.copy(bitmap = bitmap)
            _drawingList.postValue(drawings.toList())
        }
    }

    fun compareBitmapWithUrl(url: String, bitmapToCompare: Bitmap, callback: (Int) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val targetSize = 100 // Kích thước 100x100 px
                val context = getApplication<Application>().applicationContext

                // Load và resize bitmap từ URL
                val urlBitmap = withContext(Dispatchers.IO) {
                    Glide.with(context)
                        .asBitmap()
                        .load(url)
                        .submit(targetSize, targetSize)
                        .get()
                }

                // Resize bitmap cần so sánh
                val userBitmap = Bitmap.createScaledBitmap(
                    bitmapToCompare, targetSize, targetSize, true
                )

                // Tính toán phần trăm tương đồng
                val similarity = calculatePixelSimilarity(urlBitmap, userBitmap)
                callback(similarity)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(0) // Nếu lỗi thì trả 0%
            }
        }
    }

    private fun calculatePixelSimilarity(urlBitmap: Bitmap, userBitmap: Bitmap): Int {
        val width = urlBitmap.width
        val height = urlBitmap.height

        // 1. Đếm tổng pixel có màu (không trắng) trong bitmap URL
        var coloredPixelCount = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (!isTransparentOrWhite(urlBitmap.getPixel(x, y))) {
                    coloredPixelCount++
                }
            }
        }

        // Debug log
        println("Tổng pixel có màu: $coloredPixelCount")

        // Nếu không có pixel nào có màu thì trả về 0%
        if (coloredPixelCount == 0) return 0

        // 2. Tính % mỗi pixel đóng góp
        val percentPerPixel = 100.0 / coloredPixelCount
        var totalSimilarity = 0.0

        // 3. So sánh từng pixel
        for (y in 0 until height) {
            for (x in 0 until width) {
                val urlColor = urlBitmap.getPixel(x, y)

                // Chỉ xét pixel có màu trong bitmap URL
                if (!isTransparentOrWhite(urlColor)) {
                    val userColor = userBitmap.getPixel(x, y)

                    // Nếu pixel user có màu và giống màu URL
                    if (!isTransparentOrWhite(userColor) && isColorSimilar(urlColor, userColor)) {
                        totalSimilarity += percentPerPixel
                    }
                }
            }
        }

        // Đảm bảo kết quả trong khoảng 0-100
        return totalSimilarity.coerceIn(0.0, 100.0).toInt()
    }

    private fun isColorSimilar(color1: Int, color2: Int): Boolean {
        // Lấy các thành phần màu
        val r1 = Color.red(color1)
        val g1 = Color.green(color1)
        val b1 = Color.blue(color1)

        val r2 = Color.red(color2)
        val g2 = Color.green(color2)
        val b2 = Color.blue(color2)

        // Tính khoảng cách màu (có thể điều chỉnh ngưỡng)
        val colorThreshold = 150
        return abs(r1 - r2) < colorThreshold &&
                abs(g1 - g2) < colorThreshold &&
                abs(b1 - b2) < colorThreshold
    }

    private fun isTransparentOrWhite(color: Int): Boolean {
        // Kiểm tra trong suốt (alpha = 0)
        if (Color.alpha(color) == 0) return true

        // Kiểm tra màu trắng (RGB > ngưỡng)
        val whiteThreshold = 240
        return Color.red(color) > whiteThreshold &&
                Color.green(color) > whiteThreshold &&
                Color.blue(color) > whiteThreshold
    }



}