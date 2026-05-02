package com.notesapp

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/**
 * Single source of truth for note data.
 *
 * All database interactions go through here, keeping the ViewModels
 * free of Room-specific concerns. Export/import use the Storage Access
 * Framework so no runtime permissions are needed on API 29+.
 */
class NoteRepository(private val dao: NoteDao) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    // ── Read ────────────────────────────────────────────────────────────

    /** Live-updating stream of all notes, pinned first then newest first. */
    fun getAllNotes(): Flow<List<Note>> = dao.getAllNotes()

    /**
     * Live-updating stream of notes matching [query] in title or content.
     * Emits the full list when [query] is blank.
     */
    fun searchNotes(query: String): Flow<List<Note>> =
        if (query.isBlank()) dao.getAllNotes() else dao.searchNotes(query.trim())

    /** Returns a single note by [id], or null if it does not exist. */
    suspend fun getNoteById(id: Long): Note? = dao.getNoteById(id)

    // ── Write ───────────────────────────────────────────────────────────

    /**
     * Saves a note.
     *
     * @return The row ID of the inserted/updated note.
     */
    suspend fun saveNote(note: Note): Long {
        val timestamp = System.currentTimeMillis()
        return if (note.id == 0L) {
            dao.insertNote(note.copy(createdAt = timestamp, updatedAt = timestamp))
        } else {
            dao.updateNote(note.copy(updatedAt = timestamp))
            note.id
        }
    }

    /** Deletes the given note permanently. */
    suspend fun deleteNote(note: Note) = dao.deleteNote(note)

    /** Toggles the pin state of a note. */
    suspend fun togglePin(note: Note) = dao.updateNote(
        note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis())
    )

    // ── Export / Import ─────────────────────────────────────────────────

    /**
     * Exports all notes to a JSON file at [uri] (chosen by the user via SAF).
     *
     * @param context Application context for content resolver access.
     * @param uri Destination URI returned by [Intent.ACTION_CREATE_DOCUMENT].
     * @return Number of notes written.
     * @throws java.io.IOException on write failure.
     */
    suspend fun exportToJson(context: Context, uri: Uri): Int = withContext(Dispatchers.IO) {
        val notes = dao.getAllNotesSnapshot()
        val json = gson.toJson(notes)
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            OutputStreamWriter(stream, Charsets.UTF_8).use { writer ->
                writer.write(json)
            }
        } ?: error("Cannot open output stream for URI: $uri")
        notes.size
    }

    /**
     * Imports notes from a JSON file at [uri].
     *
     * When [replaceAll] is true the existing database is wiped before import.
     * When false the imported notes are merged (existing notes are kept; imported
     * notes always get new IDs to avoid collisions).
     *
     * @param context Application context for content resolver access.
     * @param uri Source URI returned by [Intent.ACTION_OPEN_DOCUMENT].
     * @param replaceAll If true, deletes all existing notes before import.
     * @return Number of notes imported.
     * @throws IllegalArgumentException if the file is not valid notes JSON.
     * @throws java.io.IOException on read failure.
     */
    suspend fun importFromJson(
        context: Context,
        uri: Uri,
        replaceAll: Boolean
    ): Int = withContext(Dispatchers.IO) {
        val json = context.contentResolver.openInputStream(uri)?.use { stream ->
            BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
        } ?: error("Cannot open input stream for URI: $uri")

        val type = object : TypeToken<List<Note>>() {}.type
        val importedNotes: List<Note> = try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid notes file: ${e.message}", e)
        }

        require(importedNotes.isNotEmpty()) { "The selected file contains no notes." }

        if (replaceAll) {
            dao.deleteAllNotes()
        }

        // Always strip IDs so Room auto-generates new ones and avoids conflicts.
        importedNotes.forEach { note ->
            dao.insertNote(note.copy(id = 0L))
        }

        importedNotes.size
    }
}
