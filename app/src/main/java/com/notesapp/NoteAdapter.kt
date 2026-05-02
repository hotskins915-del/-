package com.notesapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.notesapp.databinding.ItemNoteBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * [ListAdapter] for the notes list with DiffUtil for efficient updates.
 */
class NoteAdapter(
    private val onNoteClick: (Note) -> Unit,
    private val onNoteDelete: (Note) -> Unit,
    private val onNotePin: (Note) -> Unit
) : ListAdapter<Note, NoteAdapter.NoteViewHolder>(NoteDiffCallback()) {

    private val dateFormatter = SimpleDateFormat("d MMM, HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NoteViewHolder(private val binding: ItemNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(note: Note) {
            with(binding) {
                textTitle.text = note.title.ifBlank { "Без названия" }
                textContent.text = note.content.take(MAX_PREVIEW_CHARS)
                textDate.text = dateFormatter.format(Date(note.updatedAt))

                imagePinned.setImageResource(
                    if (note.isPinned) R.drawable.ic_pin_filled else R.drawable.ic_pin_outline
                )

                imageDrawingBadge.visibility = if (note.hasDrawing()) View.VISIBLE else View.GONE
                imagePhotoBadge.visibility = if (note.hasPhotos()) View.VISIBLE else View.GONE

                root.setOnClickListener { onNoteClick(note) }
                root.setOnLongClickListener { onNoteDelete(note); true }
                imagePinned.setOnClickListener { onNotePin(note) }
            }
        }
    }

    private class NoteDiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Note, newItem: Note) = oldItem == newItem
    }

    companion object {
        private const val MAX_PREVIEW_CHARS = 100
    }
}
