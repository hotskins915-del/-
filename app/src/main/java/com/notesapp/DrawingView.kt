package com.notesapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * A custom View that provides a freehand drawing canvas.
 *
 * Supports variable stroke width, color selection, eraser mode,
 * undo (step-by-step), and exporting the drawing as a [Bitmap].
 *
 * Usage: place in XML, call [setColor]/[setStrokeWidth]/[setEraser]
 * to configure, [undo] to revert the last stroke, and [getBitmap]
 * to obtain the result for saving.
 */
class DrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ── Rendering state ──────────────────────────────────────────────────

    private val strokeHistory: MutableList<StrokeRecord> = mutableListOf()
    private val currentPath = Path()
    private var lastX = 0f
    private var lastY = 0f

    private val currentPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = DEFAULT_STROKE_WIDTH
        color = Color.BLACK
    }

    private val canvasPaint = Paint(Paint.DITHER_FLAG)
    private lateinit var drawBitmap: Bitmap
    private lateinit var drawCanvas: Canvas

    // ── Public API ───────────────────────────────────────────────────────

    var strokeColor: Int = Color.BLACK
        set(value) {
            field = value
            currentPaint.color = value
            currentPaint.xfermode = null
        }

    var strokeWidth: Float = DEFAULT_STROKE_WIDTH
        set(value) {
            field = value
            currentPaint.strokeWidth = value
        }

    var isEraserMode: Boolean = false
        set(value) {
            field = value
            if (value) {
                currentPaint.color = Color.WHITE
                currentPaint.strokeWidth = ERASER_WIDTH
            } else {
                currentPaint.color = strokeColor
                currentPaint.strokeWidth = strokeWidth
            }
        }

    /**
     * Removes the last drawn stroke. No-op if history is empty.
     */
    fun undo() {
        if (strokeHistory.isEmpty()) return
        strokeHistory.removeAt(strokeHistory.lastIndex)
        redrawFromHistory()
    }

    /** Clears all strokes from the canvas. */
    fun clearCanvas() {
        strokeHistory.clear()
        currentPath.reset()
        if (::drawCanvas.isInitialized) {
            drawCanvas.drawColor(Color.WHITE)
        }
        invalidate()
    }

    /**
     * Returns the current drawing as a [Bitmap].
     * Returns null if the view has not yet been laid out.
     */
    fun getBitmap(): Bitmap? {
        if (!::drawBitmap.isInitialized) return null
        return drawBitmap.copy(Bitmap.Config.ARGB_8888, false)
    }

    /**
     * Loads a previously saved [Bitmap] onto the canvas.
     * The bitmap is drawn at (0,0) — caller should ensure dimensions match.
     */
    fun loadBitmap(bitmap: Bitmap) {
        if (!::drawCanvas.isInitialized) return
        drawCanvas.drawBitmap(bitmap, 0f, 0f, null)
        invalidate()
    }

    // ── View lifecycle ───────────────────────────────────────────────────

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        drawBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        drawCanvas = Canvas(drawBitmap)
        drawCanvas.drawColor(Color.WHITE)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawBitmap(drawBitmap, 0f, 0f, canvasPaint)
        canvas.drawPath(currentPath, currentPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                currentPath.moveTo(x, y)
                lastX = x
                lastY = y
            }
            MotionEvent.ACTION_MOVE -> {
                val midX = (lastX + x) / 2f
                val midY = (lastY + y) / 2f
                currentPath.quadTo(lastX, lastY, midX, midY)
                lastX = x
                lastY = y
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                currentPath.lineTo(x, y)
                val snapshotPaint = Paint(currentPaint)
                val snapshotPath = Path(currentPath)
                strokeHistory.add(StrokeRecord(snapshotPath, snapshotPaint))
                drawCanvas.drawPath(currentPath, currentPaint)
                currentPath.reset()
                invalidate()
            }
        }
        return true
    }

    // ── Private helpers ──────────────────────────────────────────────────

    private fun redrawFromHistory() {
        drawCanvas.drawColor(Color.WHITE)
        strokeHistory.forEach { record ->
            drawCanvas.drawPath(record.path, record.paint)
        }
        invalidate()
    }

    private data class StrokeRecord(val path: Path, val paint: Paint)

    companion object {
        private const val DEFAULT_STROKE_WIDTH = 8f
        private const val ERASER_WIDTH = 32f
    }
}
