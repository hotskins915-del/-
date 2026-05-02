package com.notesapp

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a single note stored in the local Room database.
 *
 * @property id Auto-generated primary key.
 * @property title Short title of the note (may be empty).
 * @property content Full text content of the note.
 * @property createdAt Unix epoch milliseconds when the note was created.
 * @property updatedAt Unix epoch milliseconds of the last modification.
 * @property isPinned Whether the note is pinned to the top of the list.
 * @property photoUris Comma-separated list of content URI strings for attached photos.
 * @property drawingPath Absolute file path to the saved drawing PNG, or empty string if none.
 * @property color Hex color string for the note card background accent.
 */
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String = "",
    val content: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val photoUris: String = "",
    val drawingPath: String = "",
    val color: String = ""
) {
    fun getPhotoUriList(): List<String> =
        if (photoUris.isBlank()) emptyList()
        else photoUris.split(",").filter { it.isNotBlank() }

    fun hasPhotos(): Boolean = photoUris.isNotBlank()
    fun hasDrawing(): Boolean = drawingPath.isNotBlank()
}
