package com.notesapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/**
 * ViewModel for [NoteEditActivity].
 *
 * Loads an existing note on start and exposes a save action.
 * The [saved] event fires with the final note ID once the write completes.
 */
class NoteEditViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NoteRepository by lazy {
        val dao = NoteDatabase.getInstance(application).noteDao()
        NoteRepository(dao)
    }

    private val _note = MutableLiveData<Note>()
    val note: LiveData<Note> = _note

    private val _saved = MutableLiveData<Long>()
    val saved: LiveData<Long> = _saved

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun loadNote(noteId: Long) {
        if (noteId == NEW_NOTE_ID) {
            _note.value = Note()
            return
        }
        viewModelScope.launch {
            val loaded = repository.getNoteById(noteId)
            if (loaded != null) {
                _note.value = loaded
            } else {
                _error.value = "Заметка не найдена"
            }
        }
    }

    /**
     * Persists the note with the given fields.
     * Discards silently if both title and content are blank.
     */
    fun saveNote(
        title: String,
        content: String,
        photoUris: String = "",
        drawingPath: String = ""
    ) {
        val base = _note.value ?: Note()
        if (title.isBlank() && content.isBlank() && photoUris.isBlank() && drawingPath.isBlank()) {
            _saved.value = EMPTY_NOTE_DISCARDED
            return
        }
        viewModelScope.launch {
            runCatching {
                repository.saveNote(
                    base.copy(
                        title = title.trim(),
                        content = content.trim(),
                        photoUris = photoUris,
                        drawingPath = drawingPath
                    )
                )
            }.onSuccess { id ->
                _saved.value = id
            }.onFailure { error ->
                _error.value = "Не удалось сохранить: ${error.message}"
            }
        }
    }

    companion object {
        const val NEW_NOTE_ID = -1L
        const val EMPTY_NOTE_DISCARDED = -2L
    }
}
