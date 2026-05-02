package com.notesapp

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

/** UI state for one-shot events (snackbar messages, dialogs). */
sealed class UiEvent {
    data class ShowMessage(val message: String) : UiEvent()
    data class ExportReady(val noteCount: Int) : UiEvent()
    data class ImportDone(val noteCount: Int) : UiEvent()
}

/**
 * ViewModel for [MainActivity].
 *
 * Survives configuration changes and mediates between the UI and [NoteRepository].
 * All coroutine work is launched in [viewModelScope] which is cancelled when the
 * ViewModel is cleared.
 */
class NoteListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NoteRepository by lazy {
        val dao = NoteDatabase.getInstance(application).noteDao()
        NoteRepository(dao)
    }

    // ── Search ──────────────────────────────────────────────────────────

    private val searchQuery = MutableStateFlow("")

    val notes: LiveData<List<Note>> = searchQuery
        .flatMapLatest { query -> repository.searchNotes(query) }
        .asLiveData()

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    // ── Events ──────────────────────────────────────────────────────────

    private val _events = MutableLiveData<UiEvent>()
    val events: LiveData<UiEvent> = _events

    // ── Actions ─────────────────────────────────────────────────────────

    fun deleteNote(note: Note) = viewModelScope.launch {
        repository.deleteNote(note)
        _events.value = UiEvent.ShowMessage("Заметка удалена")
    }

    fun togglePin(note: Note) = viewModelScope.launch {
        repository.togglePin(note)
    }

    // ── Export / Import ─────────────────────────────────────────────────

    fun exportNotes(uri: Uri) = viewModelScope.launch {
        runCatching {
            repository.exportToJson(getApplication(), uri)
        }.onSuccess { count ->
            _events.value = UiEvent.ExportReady(count)
        }.onFailure { error ->
            _events.value = UiEvent.ShowMessage("Ошибка экспорта: ${error.message}")
        }
    }

    fun importNotes(uri: Uri, replaceAll: Boolean) = viewModelScope.launch {
        runCatching {
            repository.importFromJson(getApplication(), uri, replaceAll)
        }.onSuccess { count ->
            _events.value = UiEvent.ImportDone(count)
        }.onFailure { error ->
            _events.value = UiEvent.ShowMessage("Ошибка импорта: ${error.message}")
        }
    }
}
