package com.example.multilingualnotes;


import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EditNoteActivity extends AppCompatActivity {

    private TextInputEditText editTextOriginal, editTextTranslated;
    private AutoCompleteTextView dropdownTargetLanguage;
    private MaterialButton buttonTranslate, buttonSave;
    private LinearProgressIndicator progressIndicator;
    private TextView textViewSourceLanguage;

    private FirebaseFirestore db;
    private Translator translator;

    private String noteId;
    private String sourceLanguage;
    private String targetLanguage;
    private final Map<String, String> languageCodeToName = new HashMap<>();
    private final Map<String, String> languageNameToCode = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note);

        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setTitle("Edit Note");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        editTextOriginal = findViewById(R.id.editTextOriginal);
        editTextTranslated = findViewById(R.id.editTextTranslated);
        dropdownTargetLanguage = findViewById(R.id.dropdownTargetLanguage);
        buttonTranslate = findViewById(R.id.buttonTranslate);
        buttonSave = findViewById(R.id.buttonSave);
        progressIndicator = findViewById(R.id.progressIndicator);
        textViewSourceLanguage = findViewById(R.id.textViewSourceLanguage);

        // Get note data from intent
        noteId = getIntent().getStringExtra("NOTE_ID");
        sourceLanguage = getIntent().getStringExtra("SOURCE_LANGUAGE");
        targetLanguage = getIntent().getStringExtra("TARGET_LANGUAGE");

        // Setup language maps
        setupLanguageMaps();

        // Setup dropdown for target languages
        setupLanguageDropdown();

        // Fill in existing note data
        editTextOriginal.setText(getIntent().getStringExtra("ORIGINAL_TEXT"));
        editTextTranslated.setText(getIntent().getStringExtra("TRANSLATED_TEXT"));

        // Set source language
        String sourceLanguageName = languageCodeToName.containsKey(sourceLanguage) ? languageCodeToName.get(sourceLanguage) : sourceLanguage;
        textViewSourceLanguage.setText("Source language: " + sourceLanguageName);

        // Set target language
        String targetLanguageName = languageCodeToName.containsKey(targetLanguage) ? languageCodeToName.get(targetLanguage) : targetLanguage;
        dropdownTargetLanguage.setText(targetLanguageName);

        // Setup button listeners
        setupButtonListeners();
    }

    private void setupLanguageMaps() {
        List<String> availableLanguages = TranslateLanguage.getAllLanguages();

        for (String languageCode : availableLanguages) {
            String displayName = new Locale(languageCode).getDisplayLanguage();
            languageCodeToName.put(languageCode, displayName);
            languageNameToCode.put(displayName, languageCode);
        }
    }

    private void setupLanguageDropdown() {
        List<String> languageNames = new ArrayList<>(languageNameToCode.keySet());
        java.util.Collections.sort(languageNames);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                languageNames
        );

        dropdownTargetLanguage.setAdapter(adapter);

        dropdownTargetLanguage.setOnItemClickListener((parent, view, position, id) -> {
            String selectedLanguage = (String) parent.getItemAtPosition(position);
            targetLanguage = languageNameToCode.get(selectedLanguage);
        });
    }

    private void setupButtonListeners() {
        buttonTranslate.setOnClickListener(v -> translateText());
        buttonSave.setOnClickListener(v -> saveNote());
    }

    private void translateText() {
        String originalText = editTextOriginal.getText().toString().trim();

        if (originalText.isEmpty()) {
            Toast.makeText(this, "Please enter text to translate", Toast.LENGTH_SHORT).show();
            return;
        }

        if (sourceLanguage == null || targetLanguage == null) {
            Toast.makeText(this, "Please select both source and target languages", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress
        progressIndicator.setVisibility(View.VISIBLE);
        buttonTranslate.setEnabled(false);

        // Create translator
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguage)
                .setTargetLanguage(targetLanguage)
                .build();

        translator = Translation.getClient(options);

        // Check if translation model is downloaded
        DownloadConditions conditions = new DownloadConditions.Builder()
                .requireWifi()
                .build();

        translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(v -> {
                    // Model downloaded successfully, perform translation
                    performTranslation(originalText);
                })
                .addOnFailureListener(e -> {
                    // Model download failed
                    progressIndicator.setVisibility(View.GONE);
                    buttonTranslate.setEnabled(true);
                    Toast.makeText(EditNoteActivity.this,
                            "Failed to download translation model: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void performTranslation(String text) {
        translator.translate(text)
                .addOnSuccessListener(translatedText -> {
                    // Translation successful
                    editTextTranslated.setText(translatedText);
                    progressIndicator.setVisibility(View.GONE);
                    buttonTranslate.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    // Translation failed
                    progressIndicator.setVisibility(View.GONE);
                    buttonTranslate.setEnabled(true);
                    Toast.makeText(EditNoteActivity.this,
                            "Translation failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void saveNote() {
        String originalText = editTextOriginal.getText().toString().trim();
        String translatedText = editTextTranslated.getText().toString().trim();

        if (originalText.isEmpty()) {
            Toast.makeText(this, "Original text cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress
        progressIndicator.setVisibility(View.VISIBLE);
        buttonSave.setEnabled(false);

        // Create note map
        Map<String, Object> noteMap = new HashMap<>();
        noteMap.put("originalText", originalText);
        noteMap.put("translatedText", translatedText);
        noteMap.put("sourceLanguage", sourceLanguage);
        noteMap.put("targetLanguage", targetLanguage);
        noteMap.put("lastUpdated", System.currentTimeMillis());

        // Update in Firestore
        db.collection("notes").document(noteId)
                .update(noteMap)
                .addOnSuccessListener(aVoid -> {
                    progressIndicator.setVisibility(View.GONE);
                    buttonSave.setEnabled(true);
                    Toast.makeText(EditNoteActivity.this, "Note updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressIndicator.setVisibility(View.GONE);
                    buttonSave.setEnabled(true);
                    Toast.makeText(EditNoteActivity.this,
                            "Failed to update note: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (translator != null) {
            translator.close();
        }
    }
}