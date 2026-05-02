package com.notesapp

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [Note] entities.
 *
 * All suspend functions run on the caller's coroutine context.
 * [Flow]-returning functions emit a new list on every database change.
 */
@Dao
interface NoteDao {

    /**
     * Returns all notes ordered by pin status, then by last update (newest first).
     * Emits a new list whenever any note changes.
     */
    @Query(
        """
        SELECT * FROM notes
        ORDER BY isPinned DESC, updatedAt DESC
        """
    )
    fun getAllNotes(): Flow<List<Note>>

    /**
     * Searches notes whose title or content contains [query] (case-insensitive).
     *
     * @param query The search term, without SQL wildcards — they are added here.
     */
    @Query(
        """
        SELECT * FROM notes
        WHERE title LIKE '%' || :query || '%'
           OR content LIKE '%' || :query || '%'
        ORDER BY isPinned DESC, updatedAt DESC
        """
    )
    fun searchNotes(query: String): Flow<List<Note>>

    /**
     * Returns a single note by its [id], or null if it does not exist.
     */
    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: Long): Note?

    /**
     * Inserts a new note and returns its auto-generated row ID.
     * If a note with the same primary key already exists, it is replaced.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    /**
     * Updates an existing note. The [note] must have a valid non-zero [Note.id].
     */
    @Update
    suspend fun updateNote(note: Note)

    /**
     * Deletes a specific note.
     */
    @Delete
    suspend fun deleteNote(note: Note)

    /**
     * Deletes all notes. Used only during a full import to replace existing data.
     */
    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()

    /**
     * Returns all notes as a plain list (not a Flow) for export operations.
     */
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    suspend fun getAllNotesSnapshot(): List<Note>
}
