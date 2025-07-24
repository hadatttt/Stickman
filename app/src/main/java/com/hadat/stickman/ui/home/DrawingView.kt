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
    private val stickers = mutableListOf<Sticker>()

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
    private var tempSticker: Sticker? = null
    private var isDraggingSticker = false
    private var isScalingSticker = false
    private var isRotatingSticker = false
    private var lastDistance = 0f
    private var lastAngle = 0f

    // Sticker Bitmap
    private var stickerBitmap: Bitmap? = null

    companion object {
        private const val DEFAULT_STROKE_COLOR = Color.BLACK
        private const val DEFAULT_STROKE_WIDTH = 10f
        private const val DEFAULT_ERASER_SIZE = 50f
        private const val DEFAULT_BRUSH_ALPHA = 255
        private const val TOUCH_TOLERANCE = 4f
        private const val STICKER_SIZE = 100f // Kích thước mặc định của sticker
        private const val MIN_STICKER_SIZE = 20f // Kích thước tối thiểu của sticker
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
        if (mode == Mode.STICKER && stickerBitmap != null) {
            stickers.clear()
            tempSticker = null
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
                stickers.clear()
                tempSticker = null
            }
            redrawCanvas()
        }
    }

    fun setMode(newMode: Mode) {
        if (mode == Mode.STICKER && newMode != Mode.STICKER && tempSticker != null && stickerBitmap != null) {
            commandHistory.add(StickerCommand(
                stickerBitmap!!,
                tempSticker!!.left,
                tempSticker!!.top,
                tempSticker!!.width,
                tempSticker!!.height,
                tempSticker!!.rotation
            ))
            stickers.add(tempSticker!!)
            undoneCommands.clear()
        }
        mode = newMode
        currentPaint = createPaint(
            color = strokeColor,
            strokeWidth = if (newMode == Mode.ERASE) eraserSize else strokeWidth,
            alpha = if (newMode == Mode.ERASE) 255 else brushAlpha,
            isErase = newMode == Mode.ERASE
        )
        isDraggingSticker = false
        isScalingSticker = false
        isRotatingSticker = false
        currentShapePath = null
        if (newMode == Mode.STICKER) {
            stickers.clear()
            tempSticker = null
        } else {
            tempSticker = null
        }
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
                is StickerCommand -> commandHistory.add(StickerCommand(command.bitmap, command.left, command.top, command.width, command.height, command.rotation))
            }
        }
        fillShapes.clear()
        fillShapes.addAll(state.fillShapes.map { Pair(Path(it.first), Paint(it.second)) })
        stickers.clear()
        scaleFactor = state.scaleFactor
        translateX = state.translateX
        translateY = state.translateY
        setBitmap(state.bitmap)
        if (mode == Mode.STICKER) {
            stickers.clear()
            tempSticker = null
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
        stickers.clear()
        tempSticker = null
        bitmap?.eraseColor(Color.TRANSPARENT)
        bitmapCanvas = bitmap?.let { Canvas(it) }
        invalidate()
    }

    fun undo() {
        val command = commandHistory.undo()
        if (command != null) {
            if (command is FillPathCommand) {
                fillShapes.removeAll { it.first == command.path }
            } else if (command is StickerCommand) {
                stickers.removeAll { it.left == command.left && it.top == command.top }
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
            } else if (command is StickerCommand) {
                stickers.add(Sticker(command.bitmap, command.left, command.top, command.width, command.height, command.rotation))
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
                is StickerCommand -> commandHistory.add(StickerCommand(command.bitmap, command.left, command.top, command.width, command.height, command.rotation))
            }
        }

        undoneCommands.clear()
        other.undoneCommands.forEach { command ->
            when (command) {
                is DrawPathCommand -> undoneCommands.add(DrawPathCommand(Path(command.path), Paint(command.paint)))
                is ErasePathCommand -> undoneCommands.add(ErasePathCommand(Path(command.path), Paint(command.paint)))
                is FillPathCommand -> undoneCommands.add(FillPathCommand(Path(command.path), Paint(command.paint)))
                is StickerCommand -> undoneCommands.add(StickerCommand(command.bitmap, command.left, command.top, command.width, command.height, command.rotation))
            }
        }

        fillShapes.clear()
        other.fillShapes.forEach { (path, paint) ->
            fillShapes.add(Pair(Path(path), Paint(paint)))
        }

        stickers.clear()
        other.stickers.forEach { sticker ->
            stickers.add(Sticker(sticker.bitmap, sticker.left, sticker.top, sticker.width, sticker.height, sticker.rotation))
        }

        scaleFactor = other.scaleFactor
        translateX = other.translateX
        translateY = other.translateY

        tempSticker = null
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
        for (sticker in stickers) {
            val matrix = Matrix()
            matrix.postScale(sticker.width / sticker.bitmap.width.toFloat(), sticker.height / sticker.bitmap.height.toFloat())
            matrix.postTranslate(sticker.left, sticker.top)
            matrix.postRotate(sticker.rotation, sticker.left + sticker.width / 2f, sticker.top + sticker.height / 2f)
            bitmapCanvas?.drawBitmap(sticker.bitmap, matrix, null)
        }
        for (i in 0..commandHistory.currentIndex) {
            val command = commandHistory.getCommands()[i]
            when (command) {
                is DrawPathCommand -> bitmapCanvas?.drawPath(command.path, command.paint)
                is ErasePathCommand -> bitmapCanvas?.drawPath(command.path, command.paint)
                is FillPathCommand -> bitmapCanvas?.drawPath(command.path, command.paint)
                is StickerCommand -> {
                    val matrix = Matrix()
                    matrix.postScale(command.width / command.bitmap.width.toFloat(), command.height / command.bitmap.height.toFloat())
                    matrix.postTranslate(command.left, command.top)
                    matrix.postRotate(command.rotation, command.left + command.width / 2f, command.top + command.height / 2f)
                    bitmapCanvas?.drawBitmap(command.bitmap, matrix, null)
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
        } else if (mode == Mode.STICKER && tempSticker != null && stickerBitmap != null) {
            val matrix = Matrix()
            val scale = tempSticker!!.width / stickerBitmap!!.width.toFloat()
            matrix.postScale(scale, scale)
            matrix.postTranslate(tempSticker!!.left, tempSticker!!.top)
            matrix.postRotate(tempSticker!!.rotation, tempSticker!!.left + tempSticker!!.width / 2f, tempSticker!!.top + tempSticker!!.height / 2f)
            canvas.drawBitmap(stickerBitmap!!, matrix, null)
            // Vẽ điểm góc trái trên cùng để gỡ lỗi
            val debugPaint = Paint().apply {
                color = Color.RED
                style = Paint.Style.FILL
            }
            canvas.drawCircle(tempSticker!!.left, tempSticker!!.top, 5f, debugPaint)
        }
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = (event.x - translateX) / scaleFactor
        val y = (event.y - translateY) / scaleFactor
        when (event.pointerCount) {
            1 -> handleSingleTouch(event, x, y)
            2 -> if (mode == Mode.STICKER && tempSticker != null) handleMultiTouch(event)
        }
        return true
    }

    private fun handleSingleTouch(event: MotionEvent, x: Float, y: Float) {
        isMultiTouch = false
        when (mode) {
            Mode.DRAW -> handleDraw(event, x, y)
            Mode.ERASE -> handleErase(event, x, y)
            Mode.FILL -> if (event.action == MotionEvent.ACTION_DOWN) handleFill(x, y)
            Mode.RECTANGLE, Mode.CIRCLE, Mode.LINE -> handleShapeDrawing(event, x, y)
            Mode.STICKER -> handleSticker(event, x, y)
        }
    }

    private fun handleMultiTouch(event: MotionEvent) {
        isMultiTouch = true
        val x0 = (event.getX(0) - translateX) / scaleFactor
        val y0 = (event.getY(0) - translateY) / scaleFactor
        val x1 = (event.getX(1) - translateX) / scaleFactor
        val y1 = (event.getY(1) - translateY) / scaleFactor

        when (event.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (tempSticker != null) {
                    isScalingSticker = true
                    lastDistance = calculateDistance(x0, y0, x1, y1)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isScalingSticker && tempSticker != null) {
                    // Scale
                    val newDistance = calculateDistance(x0, y0, x1, y1)
                    if (lastDistance != 0f) {
                        val scale = newDistance / lastDistance
                        tempSticker?.width = (tempSticker?.width ?: STICKER_SIZE) * scale
                        tempSticker?.height = tempSticker?.width!! // Giữ hình vuông
                        tempSticker?.width = maxOf(tempSticker!!.width, MIN_STICKER_SIZE)
                        tempSticker?.height = tempSticker?.width!!
                        lastDistance = newDistance
                    }
                    // Di chuyển góc trái trên cùng đến trung điểm hai ngón tay
                    tempSticker?.left = (x0 + x1) / 2
                    tempSticker?.top = (y0 + y1) / 2
                    invalidate()
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                isScalingSticker = false
                lastDistance = 0f
                if (tempSticker != null && stickerBitmap != null) {
                    commandHistory.add(StickerCommand(
                        stickerBitmap!!,
                        tempSticker!!.left,
                        tempSticker!!.top,
                        tempSticker!!.width,
                        tempSticker!!.height,
                        tempSticker!!.rotation
                    ))
                    stickers.add(tempSticker!!)
                    undoneCommands.clear()
                    tempSticker = null
                    isDraggingSticker = false
                    redrawCanvas()
                }
            }
        }
    }

    private fun calculateDistance(x0: Float, y0: Float, x1: Float, y1: Float): Float {
        val dx = x1 - x0
        val dy = y1 - y0
        return sqrt(dx * dx + dy * dy)
    }

    private fun calculateAngle(x0: Float, y0: Float, x1: Float, y1: Float): Float {
        val dx = x1 - x0
        val dy = y1 - y0
        return (atan2(dy, dx) * 180 / PI).toFloat()
    }

    private fun handleSticker(event: MotionEvent, x: Float, y: Float) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (stickerBitmap != null && tempSticker == null) {
                    startX = x
                    startY = y
                    tempSticker = Sticker(
                        bitmap = stickerBitmap!!,
                        left = x,
                        top = y,
                        width = STICKER_SIZE,
                        height = STICKER_SIZE,
                        rotation = 0f
                    )
                    isDraggingSticker = true
                    invalidate()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDraggingSticker && tempSticker != null) {
                    val dx = x - startX
                    val dy = y - startY

                    // Lấy kích thước chiều rộng và chiều cao theo dx và dy, giữ tối thiểu
                    val width = maxOf(abs(dx), MIN_STICKER_SIZE)
                    val height = maxOf(abs(dy), MIN_STICKER_SIZE)

                    tempSticker?.width = width
                    tempSticker?.height = height

                    tempSticker?.left = startX
                    tempSticker?.top = startY
                    invalidate()
                }
            }

            MotionEvent.ACTION_UP -> {
                if (isDraggingSticker && tempSticker != null && stickerBitmap != null) {
                    commandHistory.add(StickerCommand(
                        stickerBitmap!!,
                        tempSticker!!.left,
                        tempSticker!!.top,
                        tempSticker!!.width,
                        tempSticker!!.height,
                        tempSticker!!.rotation
                    ))
                    stickers.add(tempSticker!!)
                    undoneCommands.clear()
                    tempSticker = null
                    isDraggingSticker = false
                    redrawCanvas()
                }
            }
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
                currentShapePath = createShapePath(x, y, x, y, mode)
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
                    undoneCommands.clear()
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

    private class StickerCommand(val bitmap: Bitmap, val left: Float, val top: Float, val width: Float, val height: Float, val rotation: Float) : Command {
        override fun execute(canvas: Canvas) {
            val matrix = Matrix()
            matrix.postScale(width / bitmap.width.toFloat(), height / bitmap.height.toFloat())
            matrix.postTranslate(left, top)
            matrix.postRotate(rotation, left + width / 2f, top + height / 2f)
            canvas.drawBitmap(bitmap, matrix, null)
        }
        override fun undo(canvas: Canvas) {}
    }

    private data class Sticker(val bitmap: Bitmap, var left: Float, var top: Float, var width: Float, var height: Float, var rotation: Float)

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