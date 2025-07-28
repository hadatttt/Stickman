package com.hadat.stickman.ui.home

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

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
        DRAW, ERASE, FILL, RECTANGLE, CIRCLE, LINE, STICKER
    }

    private var bitmap: Bitmap? = null
    private var bitmapCanvas: Canvas? = null
    private var currentPaint = createPaint(DEFAULT_STROKE_COLOR, DEFAULT_STROKE_WIDTH)
    private var currentPath = Path()
    private val commandHistory = CommandHistory()
    private val undoneCommands = mutableListOf<Command>()
    private val fillShapes = mutableListOf<Pair<Path, Paint>>()

    // Thuộc tính vẽ
    private var strokeColor = DEFAULT_STROKE_COLOR
    private var strokeWidth = DEFAULT_STROKE_WIDTH
    private var eraserSize = DEFAULT_ERASER_SIZE
    private var brushAlpha = DEFAULT_BRUSH_ALPHA
    private var mode: Mode = Mode.DRAW

    // Hỗ trợ đa chạm
    private var scaleFactor = 1f
    private var translateX = 0f
    private var translateY = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isMultiTouch = false

    // Vẽ hình dạng và sticker
    private var startX = 0f
    private var startY = 0f
    private var currentShapePath: Path? = null
    private var currentStickerRect: RectF? = null // Lưu vùng sticker tạm thời

    // Sticker Bitmap
    private var stickerBitmap: Bitmap? = null

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
        setBackgroundColor(Color.TRANSPARENT)
        initializeBitmap()
    }

    private fun initializeBitmap() {
        val targetWidth = if (width > 0) width else 300
        val targetHeight = if (height > 0) height else 300
        bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        bitmapCanvas = Canvas(bitmap!!)
        bitmapCanvas?.drawColor(Color.TRANSPARENT)
    }

    fun setStickerBitmap(bitmap: Bitmap?) {
        stickerBitmap = bitmap?.copy(Bitmap.Config.ARGB_8888, true)
        if (mode == Mode.STICKER && stickerBitmap == null) {
            currentStickerRect = null
        }
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            bitmap?.recycle()
            bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bitmapCanvas = Canvas(bitmap!!)
            bitmapCanvas?.drawColor(Color.TRANSPARENT)
            if (mode == Mode.STICKER) {
                currentStickerRect = null
            }
            redrawCanvas()
        }
    }

    fun setMode(newMode: Mode) {
        mode = newMode
        currentPaint = createPaint(
            color = strokeColor,
            strokeWidth = if (newMode == Mode.ERASE) eraserSize else strokeWidth,
            alpha = if (newMode == Mode.ERASE) 255 else brushAlpha,
            isErase = newMode == Mode.ERASE
        )
        currentShapePath = null
        currentStickerRect = null
        invalidate()
    }

    fun setColor(color: Int) {
        strokeColor = color
        if (mode != Mode.ERASE) {
            currentPaint = createPaint(color, strokeWidth, brushAlpha)
        }
        invalidate()
    }

    fun setStrokeWidth(width: Float) {
        strokeWidth = width
        if (mode != Mode.ERASE) {
            currentPaint = createPaint(strokeColor, width, brushAlpha)
        }
        invalidate()
    }

    fun setEraserSize(size: Float) {
        eraserSize = size
        if (mode == Mode.ERASE) {
            currentPaint = createPaint(Color.TRANSPARENT, size, 255, isErase = true)
        }
        invalidate()
    }

    fun setBrushAlpha(alpha: Int) {
        brushAlpha = alpha.coerceIn(0, 255)
        if (mode != Mode.ERASE) {
            currentPaint = createPaint(strokeColor, strokeWidth, brushAlpha)
        }
        invalidate()
    }

    fun setBitmap(newBitmap: Bitmap?) {
        if (newBitmap != null && !newBitmap.isRecycled && newBitmap.width > 0 && newBitmap.height > 0) {
            bitmap?.recycle()
            bitmap = newBitmap.copy(Bitmap.Config.ARGB_8888, true)
            bitmapCanvas = Canvas(bitmap!!)
        } else {
            initializeBitmap()
        }
        redrawCanvas()
        invalidate()
    }

    fun setDrawingState(state: com.hadat.stickman.ui.category.DrawingViewModel.DrawingState) {
        commandHistory.clear()
        state.commands.forEach { command ->
            when (command) {
                is DrawPathCommand -> commandHistory.add(DrawPathCommand(Path(command.path), Paint(command.paint)))
                is ErasePathCommand -> commandHistory.add(ErasePathCommand(Path(command.path), Paint(command.paint)))
                is FillPathCommand -> commandHistory.add(FillPathCommand(Path(command.path), Paint(command.paint)))
                is StickerCommand -> commandHistory.add(StickerCommand(command.bitmap, command.left, command.top, command.width, command.height))
            }
        }
        fillShapes.clear()
        fillShapes.addAll(state.fillShapes.map { Pair(Path(it.first), Paint(it.second)) })
        scaleFactor = state.scaleFactor
        translateX = state.translateX
        translateY = state.translateY
        setBitmap(state.bitmap)
        if (mode == Mode.STICKER) {
            currentStickerRect = null
        }
        redrawCanvas()
        invalidate()
    }

    fun getBitmap(): Bitmap? = bitmap?.copy(Bitmap.Config.ARGB_8888, true)

    fun getCommandHistory(): List<Command> = commandHistory.getCommands()

    fun getFillShapes(): List<Pair<Path, Paint>> = fillShapes.map { Pair(Path(it.first), Paint(it.second)) }

    fun getScaleFactor(): Float = scaleFactor

    fun getTranslateX(): Float = translateX

    fun getTranslateY(): Float = translateY

    fun clearDrawing() {
        commandHistory.clear()
        undoneCommands.clear()
        fillShapes.clear()
        currentStickerRect = null
        bitmap?.eraseColor(Color.TRANSPARENT)
        bitmapCanvas = bitmap?.let { Canvas(it) }
        invalidate()
    }

    fun undo() {
        val command = commandHistory.undo()
        if (command != null) {
            if (command is FillPathCommand) {
                fillShapes.removeAll { it.first == command.path }
            }
            undoneCommands.add(command)
            redrawCanvas()
        }
    }

    fun redo() {
        if (undoneCommands.isNotEmpty()) {
            val command = undoneCommands.removeAt(undoneCommands.lastIndex)
            commandHistory.add(command)
            if (command is FillPathCommand) {
                fillShapes.add(command.path to command.paint)
            }
            redrawCanvas()
        }
    }

    fun copyFrom(other: DrawingView) {
        val otherBitmap = other.getBitmap()
        val targetWidth = if (width > 0) width else 300
        val targetHeight = if (height > 0) height else 300
        if (otherBitmap != null && !otherBitmap.isRecycled && otherBitmap.width > 0 && otherBitmap.height > 0) {
            bitmap?.recycle()
            bitmap = otherBitmap.copy(Bitmap.Config.ARGB_8888, true)
            bitmapCanvas = Canvas(bitmap!!)
        } else {
            bitmap?.recycle()
            bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            bitmapCanvas = Canvas(bitmap!!)
            bitmapCanvas?.drawColor(Color.TRANSPARENT)
        }

        strokeColor = other.strokeColor
        strokeWidth = other.strokeWidth
        eraserSize = other.eraserSize
        brushAlpha = other.brushAlpha
        mode = other.mode
        currentPaint = createPaint(
            strokeColor,
            if (mode == Mode.ERASE) eraserSize else strokeWidth,
            if (mode == Mode.ERASE) 255 else brushAlpha,
            mode == Mode.ERASE
        )

        commandHistory.clear()
        val otherCommands = other.commandHistory.getCommands()
        otherCommands.forEach { command ->
            when (command) {
                is DrawPathCommand -> commandHistory.add(DrawPathCommand(Path(command.path), Paint(command.paint)))
                is ErasePathCommand -> commandHistory.add(ErasePathCommand(Path(command.path), Paint(command.paint)))
                is FillPathCommand -> commandHistory.add(FillPathCommand(Path(command.path), Paint(command.paint)))
                is StickerCommand -> commandHistory.add(StickerCommand(command.bitmap, command.left, command.top, command.width, command.height))
            }
        }

        undoneCommands.clear()
        other.undoneCommands.forEach { command ->
            when (command) {
                is DrawPathCommand -> undoneCommands.add(DrawPathCommand(Path(command.path), Paint(command.paint)))
                is ErasePathCommand -> undoneCommands.add(ErasePathCommand(Path(command.path), Paint(command.paint)))
                is FillPathCommand -> undoneCommands.add(FillPathCommand(Path(command.path), Paint(command.paint)))
                is StickerCommand -> undoneCommands.add(StickerCommand(command.bitmap, command.left, command.top, command.width, command.height))
            }
        }

        fillShapes.clear()
        other.fillShapes.forEach { (path, paint) ->
            fillShapes.add(Pair(Path(path), Paint(paint)))
        }

        scaleFactor = other.scaleFactor
        translateX = other.translateX
        translateY = other.translateY

        currentStickerRect = null
        redrawCanvas()
        invalidate()
    }

    private fun redrawCanvas() {
        bitmap?.eraseColor(Color.TRANSPARENT)
        bitmapCanvas = bitmap?.let { Canvas(it) }
        bitmapCanvas?.save()
        bitmapCanvas?.scale(scaleFactor, scaleFactor)
        bitmapCanvas?.translate(translateX / scaleFactor, translateY / scaleFactor)
        for (shape in fillShapes) {
            bitmapCanvas?.drawPath(shape.first, shape.second)
        }
        for (i in 0..commandHistory.currentIndex) {
            val command = commandHistory.getCommands()[i]
            when (command) {
                is DrawPathCommand -> bitmapCanvas?.drawPath(command.path, command.paint)
                is ErasePathCommand -> bitmapCanvas?.drawPath(command.path, command.paint)
                is FillPathCommand -> bitmapCanvas?.drawPath(command.path, command.paint)
                is StickerCommand -> {
                    bitmapCanvas?.drawBitmap(command.bitmap, null, RectF(command.left, command.top, command.left + command.width, command.top + command.height), null)
                }
            }
        }
        bitmapCanvas?.restore()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.scale(scaleFactor, scaleFactor)
        canvas.translate(translateX / scaleFactor, translateY / scaleFactor)
        bitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
        if (mode != Mode.ERASE && mode != Mode.STICKER) {
            currentShapePath?.let { canvas.drawPath(it, currentPaint) }
            canvas.drawPath(currentPath, currentPaint)
        } else if (mode == Mode.STICKER && currentStickerRect != null && stickerBitmap != null) {
            canvas.drawBitmap(stickerBitmap!!, null, currentStickerRect!!, null)
        }
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = (event.x - translateX) / scaleFactor
        val y = (event.y - translateY) / scaleFactor
        when (event.pointerCount) {
            1 -> handleSingleTouch(event, x, y)
            2 -> isMultiTouch = true // Có thể thêm logic đa chạm nếu cần
        }
        return true
    }

    private fun handleSingleTouch(event: MotionEvent, x: Float, y: Float) {
        isMultiTouch = false
        when (mode) {
            Mode.DRAW -> handleDraw(event, x, y)
            Mode.ERASE -> handleErase(event, x, y)
            Mode.FILL -> if (event.action == MotionEvent.ACTION_DOWN) handleFill(x, y)
            Mode.RECTANGLE, Mode.CIRCLE, Mode.LINE, Mode.STICKER -> handleShapeDrawing(event, x, y)
        }
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
                undoneCommands.clear()
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
                undoneCommands.clear()
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
                if (mode == Mode.STICKER && stickerBitmap != null) {
                    currentStickerRect = RectF(startX, startY, startX, startY)
                } else {
                    currentShapePath = createShapePath(startX, startY, x, y, mode)
                    currentPaint = createPaint(strokeColor, strokeWidth, brushAlpha)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (mode == Mode.STICKER && stickerBitmap != null) {
                    currentStickerRect = RectF(
                        min(startX, x),
                        min(startY, y),
                        max(startX, x),
                        max(startY, y)
                    )
                } else {
                    currentShapePath = createShapePath(startX, startY, x, y, mode)
                }
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                if (mode == Mode.STICKER && stickerBitmap != null && currentStickerRect != null) {
                    bitmapCanvas?.drawBitmap(stickerBitmap!!, null, currentStickerRect!!, null)
                    commandHistory.add(StickerCommand(
                        stickerBitmap!!,
                        currentStickerRect!!.left,
                        currentStickerRect!!.top,
                        currentStickerRect!!.width(),
                        currentStickerRect!!.height()
                    ))
                    undoneCommands.clear()
                    currentStickerRect = null
                } else {
                    currentShapePath?.let {
                        bitmapCanvas?.drawPath(it, currentPaint)
                        commandHistory.add(DrawPathCommand(it, currentPaint))
                        undoneCommands.clear()
                    }
                    currentShapePath = null
                }
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
            undoneCommands.clear()
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
            Mode.LINE -> {
                path.moveTo(startX, startY)
                path.lineTo(endX, endY)
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
            style = if (isErase) Paint.Style.STROKE else Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
            isDither = true
            if (isErase) {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            }
        }
    }

    interface Command {
        fun execute(canvas: Canvas)
        fun undo(canvas: Canvas)
    }

    private class DrawPathCommand(val path: Path, val paint: Paint) : Command {
        override fun execute(canvas: Canvas) {
            canvas.drawPath(path, paint)
        }
        override fun undo(canvas: Canvas) {}
    }

    private class ErasePathCommand(val path: Path, val paint: Paint) : Command {
        override fun execute(canvas: Canvas) {
            canvas.drawPath(path, paint)
        }
        override fun undo(canvas: Canvas) {}
    }

    private class FillPathCommand(val path: Path, val paint: Paint) : Command {
        override fun execute(canvas: Canvas) {
            canvas.drawPath(path, paint)
        }
        override fun undo(canvas: Canvas) {}
    }

    private class StickerCommand(val bitmap: Bitmap, val left: Float, val top: Float, val width: Float, val height: Float) : Command {
        override fun execute(canvas: Canvas) {
            canvas.drawBitmap(bitmap, null, RectF(left, top, left + width, top + height), null)
        }
        override fun undo(canvas: Canvas) {}
    }

    private inner class CommandHistory {
        private val commands = mutableListOf<Command>()
        var currentIndex = -1

        fun add(command: Command) {
            while (currentIndex < commands.size - 1) {
                commands.removeAt(commands.size - 1)
            }
            commands.add(command)
            currentIndex++
            command.execute(bitmapCanvas!!)
        }

        fun undo(): Command? {
            if (currentIndex >= 0) {
                val command = commands[currentIndex]
                currentIndex--
                return command
            }
            return null
        }

        fun redo() {
            if (currentIndex < commands.size - 1) {
                currentIndex++
                commands[currentIndex].execute(bitmapCanvas!!)
            }
        }

        fun clear() {
            commands.clear()
            currentIndex = -1
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

        fun getCommands(): List<Command> = commands.take(currentIndex + 1)
    }
}