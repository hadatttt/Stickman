package com.hadat.stickman.ui.home

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * View tùy chỉnh để vẽ với hỗ trợ nhiều chế độ, hình dạng, undo/redo và cử chỉ đa chạm.
 * Nền được quản lý bởi layout phía sau, DrawingView trong suốt.
 */
class DrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class Mode {
        DRAW, ERASE, FILL, RECTANGLE, CIRCLE
    }

    private var mode: Mode = Mode.DRAW
    private var currentPath = Path()
    private var currentPaint = createPaint(DEFAULT_STROKE_COLOR, DEFAULT_STROKE_WIDTH)

    private val commandHistory = CommandHistory()
    private val fillShapes = mutableListOf<Pair<Path, Paint>>()
    private var bitmap: Bitmap? = null
    private var bitmapCanvas: Canvas? = null

    // Thuộc tính vẽ
    private var strokeColor = DEFAULT_STROKE_COLOR
    private var strokeWidth = DEFAULT_STROKE_WIDTH
    private var eraserSize = DEFAULT_ERASER_SIZE
    private var brushAlpha = DEFAULT_BRUSH_ALPHA

    // Hỗ trợ đa chạm
    private var scaleFactor = 1f
    private var translateX = 0f
    private var translateY = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isMultiTouch = false

    // Vẽ hình dạng
    private var startX = 0f
    private var startY = 0f
    private var currentShapePath: Path? = null

    companion object {
        private const val DEFAULT_STROKE_COLOR = Color.BLACK
        private const val DEFAULT_STROKE_WIDTH = 10f
        private const val DEFAULT_ERASER_SIZE = 50f
        private const val DEFAULT_BRUSH_ALPHA = 255
        private const val TOUCH_TOLERANCE = 4f
    }

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
        isFocusable = true
        isFocusableInTouchMode = true
        // Đảm bảo view trong suốt
        setBackgroundColor(Color.TRANSPARENT)
    }

    /**
     * Thiết lập chế độ vẽ (DRAW, ERASE, FILL, RECTANGLE, CIRCLE).
     */
    fun setMode(newMode: Mode) {
        mode = newMode
        if (mode == Mode.DRAW || mode == Mode.RECTANGLE || mode == Mode.CIRCLE) {
            currentPaint = createPaint(strokeColor, strokeWidth, brushAlpha)
        } else if (mode == Mode.ERASE) {
            currentPaint = createPaint(Color.TRANSPARENT, eraserSize, 255, isErase = true)
        }
    }

    /**
     * Thiết lập màu nét vẽ.
     */
    fun setColor(color: Int) {
        strokeColor = color
        if (mode == Mode.DRAW || mode == Mode.RECTANGLE || mode == Mode.CIRCLE) {
            currentPaint = createPaint(color, strokeWidth, brushAlpha)
        }
        invalidate()
    }

    /**
     * Thiết lập độ dày nét vẽ.
     */
    fun setStrokeWidth(width: Float) {
        strokeWidth = width
        if (mode == Mode.DRAW || mode == Mode.RECTANGLE || mode == Mode.CIRCLE) {
            currentPaint = createPaint(strokeColor, width, brushAlpha)
        }
        invalidate()
    }

    /**
     * Thiết lập kích thước tẩy.
     */
    fun setEraserSize(size: Float) {
        eraserSize = size
        if (mode == Mode.ERASE) {
            currentPaint = createPaint(Color.TRANSPARENT, size, 255, isErase = true)
        }
    }

    /**
     * Thiết lập độ trong suốt của bút (0-255).
     */
    fun setBrushAlpha(alpha: Int) {
        brushAlpha = alpha.coerceIn(0, 255)
        if (mode == Mode.DRAW || mode == Mode.RECTANGLE || mode == Mode.CIRCLE) {
            currentPaint = createPaint(strokeColor, strokeWidth, brushAlpha)
        }
        invalidate()
    }

    /**
     * Xóa toàn bộ canvas.
     */
    fun clearDrawing() {
        commandHistory.clear()
        fillShapes.clear()
        bitmap?.eraseColor(Color.TRANSPARENT)
        invalidate()
    }

    /**
     * Hoàn tác hành động trước đó.
     */
    fun undo() {
        commandHistory.undo()
        invalidate()
    }

    /**
     * Làm lại hành động đã hoàn tác.
     */
    fun redo() {
        commandHistory.redo()
        invalidate()
    }

    /**
     * Lấy bản vẽ dưới dạng bitmap, chỉ chứa các nét vẽ (không bao gồm nền).
     */
    fun getBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        bitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
        return bitmap
    }

    /**
     * Lấy chế độ hiện tại.
     */
    fun getMode(): Mode = mode

    /**
     * Lấy độ dày nét vẽ hiện tại.
     */
    fun getStrokeWidth(): Float = strokeWidth

    /**
     * Lấy kích thước tẩy hiện tại.
     */
    fun getEraserSize(): Float = eraserSize

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        bitmap?.recycle()
        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmapCanvas = Canvas(bitmap!!)
        bitmapCanvas?.drawColor(Color.TRANSPARENT)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Áp dụng các biến đổi để phóng to/thu nhỏ và di chuyển
        canvas.save()
        canvas.scale(scaleFactor, scaleFactor)
        canvas.translate(translateX / scaleFactor, translateY / scaleFactor)

        // Vẽ bitmap chứa các nét vẽ (trong suốt)
        bitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }

        // Chỉ vẽ currentPath hoặc currentShapePath nếu không ở chế độ ERASE
        if (mode != Mode.ERASE) {
            currentShapePath?.let { canvas.drawPath(it, currentPaint) }
            canvas.drawPath(currentPath, currentPaint)
        }

        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x / scaleFactor - translateX / scaleFactor
        val y = event.y / scaleFactor - translateY / scaleFactor

        when (event.pointerCount) {
            1 -> handleSingleTouch(event, x, y)
            2 -> handleMultiTouch(event)
        }
        return true
    }

    private fun handleSingleTouch(event: MotionEvent, x: Float, y: Float) {
        isMultiTouch = false
        when (mode) {
            Mode.DRAW -> handleDraw(event, x, y)
            Mode.ERASE -> handleErase(event, x, y)
            Mode.FILL -> if (event.action == MotionEvent.ACTION_DOWN) handleFill(x, y)
            Mode.RECTANGLE, Mode.CIRCLE -> handleShapeDrawing(event, x, y)
        }
    }

    private fun handleMultiTouch(event: MotionEvent) {
        isMultiTouch = true
        when (event.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                lastTouchX = (event.getX(0) + event.getX(1)) / 2
                lastTouchY = (event.getY(0) + event.getY(1)) / 2
            }
            MotionEvent.ACTION_MOVE -> {
                val newX = (event.getX(0) + event.getX(1)) / 2
                val newY = (event.getY(0) + event.getY(1)) / 2
                translateX += newX - lastTouchX
                translateY += newY - lastTouchY
                lastTouchX = newX
                lastTouchY = newY

                // Tính toán tỷ lệ phóng to
                val oldDist = distance(event, 0, 1)
                val newDist = distance(event, 0, 1)
                if (oldDist > 10f) {
                    scaleFactor *= newDist / oldDist
                    scaleFactor = scaleFactor.coerceIn(0.5f, 3f)
                }
                invalidate()
            }
        }
    }

    private fun distance(event: MotionEvent, first: Int, second: Int): Float {
        val dx = event.getX(first) - event.getX(second)
        val dy = event.getY(first) - event.getY(second)
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    private fun handleDraw(event: MotionEvent, x: Float, y: Float) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                currentPath = Path()
                currentPath.moveTo(x, y)
                currentPaint = createPaint(strokeColor, strokeWidth, brushAlpha)
            }
            MotionEvent.ACTION_MOVE -> {
                currentPath.lineTo(x, y)
                bitmapCanvas?.drawPath(currentPath, currentPaint)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                currentPath.lineTo(x, y)
                bitmapCanvas?.drawPath(currentPath, currentPaint)
                commandHistory.add(DrawPathCommand(currentPath, currentPaint))
                currentPath = Path()
                invalidate()
            }
        }
    }

    private fun handleErase(event: MotionEvent, x: Float, y: Float) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                currentPath = Path()
                currentPath.moveTo(x, y)
                currentPaint = createPaint(Color.TRANSPARENT, eraserSize, 255, isErase = true)
            }
            MotionEvent.ACTION_MOVE -> {
                currentPath.lineTo(x, y)
                bitmapCanvas?.drawPath(currentPath, currentPaint)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                currentPath.lineTo(x, y)
                bitmapCanvas?.drawPath(currentPath, currentPaint)
                commandHistory.add(ErasePathCommand(currentPath, currentPaint))
                currentPath = Path()
                invalidate()
            }
        }
    }

    private fun handleShapeDrawing(event: MotionEvent, x: Float, y: Float) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = x
                startY = y
                currentShapePath = Path()
                currentPaint = createPaint(strokeColor, strokeWidth, brushAlpha)
            }
            MotionEvent.ACTION_MOVE -> {
                currentShapePath = createShapePath(startX, startY, x, y, mode)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                currentShapePath?.let {
                    bitmapCanvas?.drawPath(it, currentPaint)
                    commandHistory.add(DrawPathCommand(it, currentPaint))
                }
                currentShapePath = null
                invalidate()
            }
        }
    }

    private fun handleFill(x: Float, y: Float) {
        val targetPath = commandHistory.findPathAt(x, y)
        if (targetPath != null) {
            val paintFill = Paint().apply {
                style = Paint.Style.FILL
                color = strokeColor
                alpha = brushAlpha
                isAntiAlias = true
            }
            fillShapes.add(targetPath.first to paintFill)
            bitmapCanvas?.drawPath(targetPath.first, paintFill)
            commandHistory.add(FillPathCommand(targetPath.first, paintFill))
            invalidate()
        }
    }

    private fun createShapePath(startX: Float, startY: Float, endX: Float, endY: Float, mode: Mode): Path {
        val path = Path()
        when (mode) {
            Mode.RECTANGLE -> {
                val left = min(startX, endX)
                val right = max(startX, endX)
                val top = min(startY, endY)
                val bottom = max(startY, endY)
                path.addRect(left, top, right, bottom, Path.Direction.CW)
            }
            Mode.CIRCLE -> {
                val radius = abs(startX - endX).coerceAtLeast(abs(startY - endY)) / 2
                val centerX = (startX + endX) / 2
                val centerY = (startY + endY) / 2
                path.addCircle(centerX, centerY, radius, Path.Direction.CW)
            }
            else -> {}
        }
        return path
    }

    private fun createPaint(color: Int, strokeWidth: Float, alpha: Int = 255, isErase: Boolean = false): Paint {
        return Paint().apply {
            this.color = color
            this.strokeWidth = strokeWidth
            this.alpha = alpha
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
            isDither = true
            if (isErase) {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            }
        }
    }

    private interface Command {
        fun execute(canvas: Canvas)
        fun undo(canvas: Canvas)
    }

    private class DrawPathCommand(val path: Path, val paint: Paint) : Command {
        override fun execute(canvas: Canvas) {
            canvas.drawPath(path, paint)
        }
        override fun undo(canvas: Canvas) {
            // Vẽ lại toàn bộ canvas
        }
    }

    private class ErasePathCommand(private val path: Path, private val paint: Paint) : Command {
        override fun execute(canvas: Canvas) {
            canvas.drawPath(path, paint)
        }
        override fun undo(canvas: Canvas) {
            // Vẽ lại toàn bộ canvas
        }
    }

    private class FillPathCommand(private val path: Path, private val paint: Paint) : Command {
        override fun execute(canvas: Canvas) {
            canvas.drawPath(path, paint)
        }
        override fun undo(canvas: Canvas) {
            // Vẽ lại toàn bộ canvas
        }
    }

    private inner class CommandHistory {
        private val commands = mutableListOf<Command>()
        private var currentIndex = -1

        fun add(command: Command) {
            while (currentIndex < commands.size - 1) {
                commands.removeAt(commands.size - 1)
            }
            commands.add(command)
            currentIndex++
            command.execute(bitmapCanvas!!)
        }

        fun undo() {
            if (currentIndex >= 0) {
                bitmap?.eraseColor(Color.TRANSPARENT)
                fillShapes.clear()
                for (i in 0 until currentIndex) {
                    commands[i].execute(bitmapCanvas!!)
                }
                currentIndex--
                invalidate()
            }
        }

        fun redo() {
            if (currentIndex < commands.size - 1) {
                currentIndex++
                commands[currentIndex].execute(bitmapCanvas!!)
                invalidate()
            }
        }

        fun clear() {
            commands.clear()
            currentIndex = -1
            bitmap?.eraseColor(Color.TRANSPARENT)
        }

        fun findPathAt(x: Float, y: Float): Pair<Path, Paint>? {
            for (i in commands.size - 1 downTo 0) {
                val command = commands[i]
                if (command is DrawPathCommand) {
                    val path = command.path
                    val bounds = RectF()
                    path.computeBounds(bounds, true)
                    val region = Region()
                    region.setPath(
                        path,
                        Region(bounds.left.toInt(), bounds.top.toInt(), bounds.right.toInt(), bounds.bottom.toInt())
                    )
                    if (region.contains(x.toInt(), y.toInt())) {
                        return path to command.paint
                    }
                }
            }
            return null
        }
    }
}