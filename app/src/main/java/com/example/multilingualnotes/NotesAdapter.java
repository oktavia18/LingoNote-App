package com.example.multilingualnotes;



import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {

    private List<Note> notesList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
    private OnNoteClickListener listener;

    public interface OnNoteClickListener {
        void onNoteClick(Note note);
    }

    public NotesAdapter(List<Note> notesList, OnNoteClickListener listener) {
        this.notesList = notesList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = notesList.get(position);

        holder.textViewOriginal.setText(note.getOriginalText());
        holder.textViewTranslated.setText(note.getTranslatedText());

        String sourceLanguage = new Locale(note.getSourceLanguage()).getDisplayLanguage();
        String targetLanguage = new Locale(note.getTargetLanguage()).getDisplayLanguage();

        holder.textViewLanguages.setText(sourceLanguage + " → " + targetLanguage);

        if (note.getTimestamp() != null) {
            Date date = note.getTimestamp().toDate();
            holder.textViewDate.setText(dateFormat.format(date));
        } else {
            holder.textViewDate.setText("Unknown date");
        }

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNoteClick(note);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notesList.size();
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView textViewOriginal, textViewTranslated, textViewLanguages, textViewDate;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewOriginal = itemView.findViewById(R.id.textViewOriginal);
            textViewTranslated = itemView.findViewById(R.id.textViewTranslated);
            textViewLanguages = itemView.findViewById(R.id.textViewLanguages);
            textViewDate = itemView.findViewById(R.id.textViewDate);
        }
    }
}