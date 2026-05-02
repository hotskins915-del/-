package com.notesapp

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.notesapp.databinding.ActivityDrawingBinding
import java.io.File
import java.io.FileOutputStream

/**
 * Full-screen drawing canvas activity.
 *
 * Accepts an optional [EXTRA_DRAWING_PATH] to load an existing drawing.
 * On save, writes the bitmap to a PNG file in the app's files directory
 * and returns the path via [RESULT_DRAWING_PATH].
 */
class DrawingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDrawingBinding
    private var existingPath: String = ""

    private val palette = listOf(
        "#111111", "#FFFFFF", "#F0C040", "#E84040",
        "#4CAF50", "#2196F3", "#9C27B0", "#FF5722"
    )
    private var selectedIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDrawingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        existingPath = intent.getStringExtra(EXTRA_DRAWING_PATH).orEmpty()
        loadExistingDrawing()
        setupColorPalette()
        setupControls()
    }

    private fun loadExistingDrawing() {
        if (existingPath.isBlank()) return
        val file = File(existingPath)
        if (!file.exists()) return
        val bitmap = BitmapFactory.decodeFile(existingPath) ?: return
        binding.drawingView.post { binding.drawingView.loadBitmap(bitmap) }
    }

    private fun setupColorPalette() {
        val dp = resources.displayMetrics.density
        val sizePx = (40 * dp).toInt()
        val marginPx = (8 * dp).toInt()

        palette.forEachIndexed { index, hex ->
            val dot = android.view.View(this).apply {
                layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).also {
                    it.marginEnd = marginPx
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor(hex))
                    if (hex == "#FFFFFF") setStroke((2 * dp).toInt(), Color.parseColor("#AAAAAA"))
                }
                setOnClickListener {
                    selectedIndex = index
                    binding.drawingView.isEraserMode = false
                    binding.drawingView.strokeColor = Color.parseColor(hex)
                    updatePaletteSelection()
                }
            }
            binding.colorContainer.addView(dot)
        }
        updatePaletteSelection()
    }

    private fun updatePaletteSelection() {
        val dp = resources.displayMetrics.density
        for (i in 0 until binding.colorContainer.childCount) {
            val v = binding.colorContainer.getChildAt(i)
            val selected = i == selectedIndex
            v.scaleX = if (selected) 1.25f else 1f
            v.scaleY = if (selected) 1.25f else 1f
            (v.background as? GradientDrawable)?.setStroke(
                if (selected) (3 * dp).toInt() else 0,
                Color.parseColor("#F0C040")
            )
        }
    }

    private fun setupControls() {
        binding.seekBrushSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                binding.drawingView.strokeWidth = (progress + 2).toFloat()
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })
        binding.seekBrushSize.progress = 6

        binding.btnEraser.setOnClickListener {
            binding.drawingView.isEraserMode = !binding.drawingView.isEraserMode
            binding.btnEraser.alpha = if (binding.drawingView.isEraserMode) 1f else 0.6f
            if (!binding.drawingView.isEraserMode) {
                binding.drawingView.strokeColor = Color.parseColor(palette[selectedIndex])
            }
        }
        binding.btnUndo.setOnClickListener { binding.drawingView.undo() }
        binding.btnClear.setOnClickListener { binding.drawingView.clearCanvas() }
        binding.btnSave.setOnClickListener { saveDrawing() }
        binding.btnCancel.setOnClickListener { finish() }
    }

    private fun saveDrawing() {
        val bitmap = binding.drawingView.getBitmap() ?: return
        val file = if (existingPath.isNotBlank()) File(existingPath)
        else File(filesDir, "drawing_${System.currentTimeMillis()}.png")

        FileOutputStream(file).use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
        }
        setResult(RESULT_OK, Intent().putExtra(RESULT_DRAWING_PATH, file.absolutePath))
        finish()
    }

    companion object {
        const val EXTRA_DRAWING_PATH = "extra_drawing_path"
        const val RESULT_DRAWING_PATH = "result_drawing_path"
    }
}
