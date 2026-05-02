package com.notesapp

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.snackbar.Snackbar
import com.notesapp.databinding.ActivityNoteEditBinding
import java.io.File
import java.io.InputStream

/**
 * Full-screen editor for creating and modifying a single note.
 *
 * Supports:
 *  - Text editing (title + body)
 *  - Attaching photos from gallery or camera
 *  - Opening the drawing canvas ([DrawingActivity]) and embedding the result
 *  - Auto-save on back navigation; empty notes are silently discarded
 */
class NoteEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoteEditBinding
    private val viewModel: NoteEditViewModel by viewModels()

    private val attachedPhotoUris = mutableListOf<String>()
    private var drawingPath: String = ""
    private var cameraPhotoUri: Uri? = null

    // ── Activity result launchers ────────────────────────────────────────

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { uri ->
            persistPhotoUri(uri)
            addPhotoView(uri.toString())
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraPhotoUri?.let { uri ->
                attachedPhotoUris.add(uri.toString())
                addPhotoView(uri.toString())
            }
        }
    }

    private val drawingLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val path = result.data?.getStringExtra(DrawingActivity.RESULT_DRAWING_PATH)
            if (!path.isNullOrBlank()) {
                drawingPath = path
                showDrawingPreview(path)
            }
        }
    }

    // ── Lifecycle ────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val noteId = intent.getLongExtra(EXTRA_NOTE_ID, NoteEditViewModel.NEW_NOTE_ID)
        viewModel.loadNote(noteId)

        observeViewModel()
        setupAttachmentButtons()

        onBackPressedDispatcher.addCallback(this) { saveAndFinish() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_edit, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> { saveAndFinish(); true }
        R.id.action_save -> { saveAndFinish(); true }
        else -> super.onOptionsItemSelected(item)
    }

    // ── Private helpers ──────────────────────────────────────────────────

    private fun observeViewModel() {
        viewModel.note.observe(this) { note ->
            if (binding.editTitle.text.isNullOrEmpty() &&
                binding.editContent.text.isNullOrEmpty()
            ) {
                binding.editTitle.setText(note.title)
                binding.editContent.setText(note.content)
                binding.editContent.requestFocus()

                // Restore photos
                note.getPhotoUriList().forEach { uriStr ->
                    attachedPhotoUris.add(uriStr)
                    addPhotoView(uriStr)
                }

                // Restore drawing
                if (note.hasDrawing()) {
                    drawingPath = note.drawingPath
                    showDrawingPreview(note.drawingPath)
                }
            }
        }

        viewModel.saved.observe(this) { finish() }

        viewModel.error.observe(this) { message ->
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun setupAttachmentButtons() {
        binding.btnAddPhoto.setOnClickListener {
            galleryLauncher.launch(arrayOf("image/*"))
        }

        binding.btnCamera.setOnClickListener {
            val photoFile = File(filesDir, "camera_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", photoFile)
            cameraPhotoUri = uri
            cameraLauncher.launch(uri)
        }

        binding.btnDraw.setOnClickListener {
            val intent = Intent(this, DrawingActivity::class.java)
                .putExtra(DrawingActivity.EXTRA_DRAWING_PATH, drawingPath)
            drawingLauncher.launch(intent)
        }
    }

    private fun persistPhotoUri(uri: Uri) {
        contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        attachedPhotoUris.add(uri.toString())
    }

    private fun addPhotoView(uriStr: String) {
        val imageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                resources.displayMetrics.widthPixels - 64,
                600
            ).also { it.bottomMargin = 12 }
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(0xFF1A1A1A.toInt())
        }

        runCatching {
            val uri = Uri.parse(uriStr)
            val stream: InputStream? = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(stream)
            imageView.setImageBitmap(bitmap)
        }.onFailure {
            // URI from camera file path fallback
            runCatching {
                imageView.setImageURI(Uri.parse(uriStr))
            }
        }

        binding.photosContainer.addView(imageView)
        binding.photosContainer.visibility = View.VISIBLE
    }

    private fun showDrawingPreview(path: String) {
        runCatching {
            val bitmap = BitmapFactory.decodeFile(path) ?: return
            binding.drawingPreview.setImageBitmap(bitmap)
            binding.drawingPreview.visibility = View.VISIBLE
            binding.drawingPreview.setOnClickListener {
                val intent = Intent(this, DrawingActivity::class.java)
                    .putExtra(DrawingActivity.EXTRA_DRAWING_PATH, path)
                drawingLauncher.launch(intent)
            }
        }
    }

    private fun saveAndFinish() {
        viewModel.saveNote(
            title = binding.editTitle.text?.toString().orEmpty(),
            content = binding.editContent.text?.toString().orEmpty(),
            photoUris = attachedPhotoUris.joinToString(","),
            drawingPath = drawingPath
        )
    }

    companion object {
        const val EXTRA_NOTE_ID = "extra_note_id"
    }
}
