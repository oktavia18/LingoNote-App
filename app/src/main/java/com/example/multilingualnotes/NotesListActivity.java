package com.example.multilingualnotes;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;


import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class NotesListActivity extends AppCompatActivity implements NotesAdapter.OnNoteClickListener {

    private RecyclerView recyclerViewNotes;
    private LinearProgressIndicator progressIndicator;
    private TextView textViewEmpty;

    private FirebaseFirestore db;
    private NotesAdapter notesAdapter;
    private List<Note> notesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes_list);

        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setTitle("My Notes");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        recyclerViewNotes = findViewById(R.id.recyclerViewNotes);
        progressIndicator = findViewById(R.id.progressIndicator);
        textViewEmpty = findViewById(R.id.textViewEmpty);

        // Set up RecyclerView
        notesList = new ArrayList<>();
        notesAdapter = new NotesAdapter(notesList, this);

        recyclerViewNotes.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewNotes.setAdapter(notesAdapter);

        // Load notes
        loadNotes();
    }

    private void loadNotes() {
        progressIndicator.setVisibility(View.VISIBLE);

        db.collection("notes")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressIndicator.setVisibility(View.GONE);

                    if (queryDocumentSnapshots.isEmpty()) {
                        textViewEmpty.setVisibility(View.VISIBLE);
                        recyclerViewNotes.setVisibility(View.GONE);
                    } else {
                        textViewEmpty.setVisibility(View.GONE);
                        recyclerViewNotes.setVisibility(View.VISIBLE);

                        notesList.clear();

                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            Note note = document.toObject(Note.class);
                            note.setId(document.getId());
                            notesList.add(note);
                        }

                        notesAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    progressIndicator.setVisibility(View.GONE);
                    textViewEmpty.setText("Error loading notes: " + e.getMessage());
                    textViewEmpty.setVisibility(View.VISIBLE);
                    recyclerViewNotes.setVisibility(View.GONE);
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onNoteClick(Note note) {
        // Show options dialog
        String[] options = {"Edit", "Delete", "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Note Options")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Edit
                            editNote(note);
                            break;
                        case 1: // Delete
                            confirmDelete(note);
                            break;
                        case 2: // Cancel
                            dialog.dismiss();
                            break;
                    }
                })
                .show();
    }

    private void editNote(Note note) {
        if (note == null || note.getId() == null) {
            Toast.makeText(this, "Error: Note not found", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, EditNoteActivity.class);
        intent.putExtra("NOTE_ID", note.getId());
        intent.putExtra("ORIGINAL_TEXT", note.getOriginalText() != null ? note.getOriginalText() : "");
        intent.putExtra("TRANSLATED_TEXT", note.getTranslatedText() != null ? note.getTranslatedText() : "");
        intent.putExtra("SOURCE_LANGUAGE", note.getSourceLanguage() != null ? note.getSourceLanguage() : "");
        intent.putExtra("TARGET_LANGUAGE", note.getTargetLanguage() != null ? note.getTargetLanguage() : "");
        startActivity(intent);
    }


    private void confirmDelete(Note note) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to delete this note?")
                .setPositiveButton("Delete", (dialog, which) -> deleteNote(note))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteNote(Note note) {
        progressIndicator.setVisibility(View.VISIBLE);

        db.collection("notes").document(note.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    progressIndicator.setVisibility(View.GONE);
                    Toast.makeText(this, "Note deleted successfully", Toast.LENGTH_SHORT).show();

                    // Remove from list and update adapter
                    notesList.remove(note);
                    notesAdapter.notifyDataSetChanged();

                    // Show empty view if no notes left
                    if (notesList.isEmpty()) {
                        textViewEmpty.setVisibility(View.VISIBLE);
                        recyclerViewNotes.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    progressIndicator.setVisibility(View.GONE);
                    Toast.makeText(this, "Error deleting note: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}