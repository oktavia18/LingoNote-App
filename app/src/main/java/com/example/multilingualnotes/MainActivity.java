package com.example.multilingualnotes;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText editTextOriginal, editTextTranslated;
    private AutoCompleteTextView dropdownTargetLanguage;
    private MaterialButton buttonTranslate, buttonSave, buttonViewNotes;
    private LinearProgressIndicator progressIndicator;
    private TextView textViewDetectedLanguage;

    private LanguageIdentifier languageIdentifier;
    private Translator translator;
    private FirebaseFirestore db;

    private String detectedLanguageCode = "";
    private String selectedTargetLanguageCode = "";
    private final Map<String, String> languageCodeToName = new HashMap<>();
    private final Map<String, String> languageNameToCode = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();



        // Initialize ML Kit Language Identifier
        languageIdentifier = LanguageIdentification.getClient();

        // Initialize UI elements
        editTextOriginal = findViewById(R.id.editTextOriginal);
        editTextTranslated = findViewById(R.id.editTextTranslated);
        dropdownTargetLanguage = findViewById(R.id.dropdownTargetLanguage);
        buttonTranslate = findViewById(R.id.buttonTranslate);
        buttonSave = findViewById(R.id.buttonSave);
        buttonViewNotes = findViewById(R.id.buttonViewNotes);
        progressIndicator = findViewById(R.id.progressIndicator);
        textViewDetectedLanguage = findViewById(R.id.textViewDetectedLanguage);

        // Setup language maps
        setupLanguageMaps();

        // Setup dropdown for target languages
        setupLanguageDropdown();

        // Setup text change listener for language detection
        setupTextChangeListener();

        // Setup button click listeners
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
            selectedTargetLanguageCode = languageNameToCode.get(selectedLanguage);
        });
    }

    private void setupTextChangeListener() {
        editTextOriginal.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not needed
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 10) { // Only detect language for text with sufficient length
                    detectLanguage(s.toString());
                } else {
                    textViewDetectedLanguage.setText("Detected language: None");
                    detectedLanguageCode = "";
                }
            }
        });
    }

    private void setupButtonListeners() {
        buttonTranslate.setOnClickListener(v -> translateText());

        buttonSave.setOnClickListener(v -> saveNote());

        buttonViewNotes.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NotesListActivity.class);
            startActivity(intent);
        });
    }

    private void detectLanguage(String text) {
        languageIdentifier.identifyLanguage(text)
                .addOnSuccessListener(languageCode -> {
                    if (languageCode.equals("und")) {
                        textViewDetectedLanguage.setText("Detected language: Unknown");
                        detectedLanguageCode = "";
                    } else {
                        String languageName = languageCodeToName.getOrDefault(languageCode, languageCode);
                        textViewDetectedLanguage.setText("Detected language: " + languageName);
                        detectedLanguageCode = languageCode;
                    }
                })
                .addOnFailureListener(e -> {
                    textViewDetectedLanguage.setText("Language detection failed");
                    detectedLanguageCode = "";
                });
    }

    private void translateText() {
        String originalText = editTextOriginal.getText().toString().trim();

        if (originalText.isEmpty()) {
            Toast.makeText(this, "Please enter text to translate", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedTargetLanguageCode.isEmpty()) {
            Toast.makeText(this, "Please select a target language", Toast.LENGTH_SHORT).show();
            return;
        }

        if (detectedLanguageCode.isEmpty()) {
            Toast.makeText(this, "Language detection in progress or failed", Toast.LENGTH_SHORT).show();
            return;
        }

        if (detectedLanguageCode.equals(selectedTargetLanguageCode)) {
            editTextTranslated.setText(originalText);
            Toast.makeText(this, "Source and target languages are the same", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create translator with detected source language and selected target language
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(detectedLanguageCode)
                .setTargetLanguage(selectedTargetLanguageCode)
                .build();

        translator = Translation.getClient(options);

        // Show progress indicator
        progressIndicator.setVisibility(View.VISIBLE);

        // Use alternative to API level 24 requirement
        DownloadConditions conditions = new DownloadConditions.Builder()
                .requireWifi()
                .build();

        translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(unused -> {
                    // Model downloaded successfully, translate text
                    translator.translate(originalText)
                            .addOnSuccessListener(translatedText -> {
                                editTextTranslated.setText(translatedText);
                                progressIndicator.setVisibility(View.GONE);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(MainActivity.this, "Translation failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                progressIndicator.setVisibility(View.GONE);
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Model download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressIndicator.setVisibility(View.GONE);
                });
    }

    private void saveNote() {
        String originalText = editTextOriginal.getText().toString().trim();
        String translatedText = editTextTranslated.getText().toString().trim();

        if (originalText.isEmpty()) {
            Toast.makeText(this, "Please enter some text for your note", Toast.LENGTH_SHORT).show();
            return;
        }

        if (translatedText.isEmpty()) {
            Toast.makeText(this, "Please translate your text before saving", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress indicator
        progressIndicator.setVisibility(View.VISIBLE);

        // Create note object
        Map<String, Object> note = new HashMap<>();
        note.put("originalText", originalText);
        note.put("translatedText", translatedText);
        note.put("sourceLanguage", detectedLanguageCode);
        note.put("targetLanguage", selectedTargetLanguageCode);
        note.put("timestamp", new Timestamp(new Date()));

        // Save to Firebase
        db.collection("notes")
                .add(note)
                .addOnSuccessListener(documentReference -> {
                    progressIndicator.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "Note saved successfully", Toast.LENGTH_SHORT).show();

                    // Clear inputs
                    editTextOriginal.setText("");
                    editTextTranslated.setText("");
                    dropdownTargetLanguage.setText("");
                    textViewDetectedLanguage.setText("Detected language: None");

                    // Navigate to notes list
                    Intent intent = new Intent(MainActivity.this, NotesListActivity.class);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    progressIndicator.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "Failed to save note: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Close translator when activity is destroyed
        if (translator != null) {
            translator.close();
        }
    }
}