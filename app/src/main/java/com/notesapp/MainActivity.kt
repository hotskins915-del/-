package com.notesapp

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.text.format.DateFormat
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.notesapp.databinding.ActivityMainBinding
import java.util.Date

/**
 * Entry point of the application.
 *
 * Displays the full list of notes, handles search, and provides
 * actions for creating, deleting, exporting, importing, and
 * switching between light and dark themes.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences
    private val viewModel: NoteListViewModel by viewModels()

    private val adapter = NoteAdapter(
        onNoteClick = { note -> openEditor(note.id) },
        onNoteDelete = { note -> confirmDelete(note) },
        onNotePin = { note -> viewModel.togglePin(note) }
    )

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? -> uri?.let { viewModel.exportNotes(it) } }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { confirmImportMode(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        applyThemeFromPrefs()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        setupRecyclerView()
        observeViewModel()

        binding.fabNewNote.setOnClickListener { openEditor(NoteEditViewModel.NEW_NOTE_ID) }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.queryHint = getString(R.string.search_hint)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String) = true
            override fun onQueryTextChange(newText: String): Boolean {
                viewModel.setSearchQuery(newText)
                return true
            }
        })
        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem) = true
            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                viewModel.setSearchQuery("")
                return true
            }
        })

        // Set correct theme icon on launch
        val isDark = prefs.getBoolean(KEY_DARK_MODE, false)
        menu.findItem(R.id.action_theme)?.setIcon(
            if (isDark) R.drawable.ic_light_mode else R.drawable.ic_dark_mode
        )

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_theme -> {
            toggleTheme(item)
            true
        }
        R.id.action_export -> {
            val filename = "notes_${DateFormat.format("yyyyMMdd_HHmm", Date())}.json"
            exportLauncher.launch(filename)
            true
        }
        R.id.action_import -> {
            importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    // ── Private helpers ──────────────────────────────────────────────────

    private fun setupRecyclerView() {
        binding.recyclerNotes.layoutManager = LinearLayoutManager(this)
        binding.recyclerNotes.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.notes.observe(this) { notes ->
            adapter.submitList(notes)
            binding.textEmpty.visibility =
                if (notes.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }

        viewModel.events.observe(this) { event ->
            when (event) {
                is UiEvent.ShowMessage ->
                    Snackbar.make(binding.root, event.message, Snackbar.LENGTH_SHORT).show()
                is UiEvent.ExportReady ->
                    Snackbar.make(
                        binding.root,
                        "Экспортировано: ${event.noteCount} заметок",
                        Snackbar.LENGTH_LONG
                    ).show()
                is UiEvent.ImportDone ->
                    Snackbar.make(
                        binding.root,
                        "Импортировано: ${event.noteCount} заметок",
                        Snackbar.LENGTH_LONG
                    ).show()
            }
        }
    }

    private fun toggleTheme(item: MenuItem) {
        val isDark = prefs.getBoolean(KEY_DARK_MODE, false)
        val newDark = !isDark
        prefs.edit().putBoolean(KEY_DARK_MODE, newDark).apply()
        AppCompatDelegate.setDefaultNightMode(
            if (newDark) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
        item.setIcon(if (newDark) R.drawable.ic_light_mode else R.drawable.ic_dark_mode)
    }

    private fun applyThemeFromPrefs() {
        val isDark = prefs.getBoolean(KEY_DARK_MODE, false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun openEditor(noteId: Long) {
        startActivity(
            Intent(this, NoteEditActivity::class.java)
                .putExtra(NoteEditActivity.EXTRA_NOTE_ID, noteId)
        )
    }

    private fun confirmDelete(note: Note) {
        AlertDialog.Builder(this)
            .setTitle("Удалить заметку?")
            .setMessage(note.title.ifBlank { "Без названия" })
            .setPositiveButton("Удалить") { _, _ -> viewModel.deleteNote(note) }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun confirmImportMode(uri: Uri) {
        AlertDialog.Builder(this)
            .setTitle("Импорт заметок")
            .setMessage("Как обработать существующие заметки?")
            .setPositiveButton("Заменить все") { _, _ ->
                viewModel.importNotes(uri, replaceAll = true)
            }
            .setNeutralButton("Добавить") { _, _ ->
                viewModel.importNotes(uri, replaceAll = false)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    companion object {
        private const val PREFS_NAME = "notes_prefs"
        private const val KEY_DARK_MODE = "dark_mode"
    }
}
