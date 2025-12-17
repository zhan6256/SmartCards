package com.daclink.mydemoapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.daclink.mydemoapplication.Database.FlashcardDAO;
import com.daclink.mydemoapplication.Database.GymLogDatabase;
import com.daclink.mydemoapplication.Database.entities.Flashcard;

import java.util.ArrayList;
import java.util.List;

/*
 * Author: France Zhang
 * Created on: 12/17/2025
 * Description: FlashcardsActivity class
 */


public class FlashcardsActivity extends AppCompatActivity {

    private static final String EXTRA_COURSE_ID =
            "com.daclink.mydemoapplication.EXTRA_COURSE_ID";
    private static final String EXTRA_COURSE_NAME =
            "com.daclink.mydemoapplication.EXTRA_COURSE_NAME";

    private int courseId = -1;
    private String courseName = "";

    private TextView titleTextView;
    private TextView positionTextView;
    private TextView cardTextView;

    private EditText editQuestionEditText;   // NEW
    private EditText editAnswerEditText;     //  NEW

    private Button addCardButton;
    private Button editCardButton;           // NEW
    private Button deleteCardButton;
    private Button saveCardButton;           // NEW
    private Button leftArrowButton;
    private Button rightArrowButton;

    private final List<Flashcard> cards = new ArrayList<>();
    private int currentIndex = 0;
    private boolean showingQuestion = true;

    private boolean isAdmin = false;
    private boolean isEditing = false;

    public static Intent flashcardsIntentFactory(Context context, int courseId, String courseName) {
        Intent intent = new Intent(context, FlashcardsActivity.class);
        intent.putExtra(EXTRA_COURSE_ID, courseId);
        intent.putExtra(EXTRA_COURSE_NAME, courseName);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcards);

        // Back arrow
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        courseId = getIntent().getIntExtra(EXTRA_COURSE_ID, -1);
        courseName = getIntent().getStringExtra(EXTRA_COURSE_NAME);
        if (courseName == null) courseName = "";

        // Views
        titleTextView = findViewById(R.id.flashcardsTitleTextView);
        positionTextView = findViewById(R.id.positionTextView);
        cardTextView = findViewById(R.id.cardTextView);

        editQuestionEditText = findViewById(R.id.editQuestionEditText);
        editAnswerEditText = findViewById(R.id.editAnswerEditText);

        addCardButton = findViewById(R.id.addCardButton);
        editCardButton = findViewById(R.id.editCardButton);
        deleteCardButton = findViewById(R.id.deleteCardButton);
        saveCardButton = findViewById(R.id.saveCardButton);

        leftArrowButton = findViewById(R.id.leftArrowButton);
        rightArrowButton = findViewById(R.id.rightArrowButton);

        titleTextView.setText("Flashcards for: " + courseName);

        // ADMIN-ONLY: hide Add/Edit/Delete/Save for non-admin users
        SharedPreferences prefs =
                getSharedPreferences(MainActivity.SHARED_PREFERENCE_USERID_KEY, MODE_PRIVATE);
        isAdmin = prefs.getBoolean("IS_ADMIN", false);

        if (!isAdmin) {
            addCardButton.setVisibility(View.GONE);
            editCardButton.setVisibility(View.GONE);
            deleteCardButton.setVisibility(View.GONE);
            saveCardButton.setVisibility(View.GONE);

            // also ensure edit fields are hidden
            editQuestionEditText.setVisibility(View.GONE);
            editAnswerEditText.setVisibility(View.GONE);
        }

        // Tap card to flip Q <-> A (only in view mode)
        cardTextView.setOnClickListener(v -> {
            if (isEditing) return;
            if (cards.isEmpty()) return;
            showingQuestion = !showingQuestion;
            updateCardDisplay();
        });

        // Navigate left (only in view mode)
        leftArrowButton.setOnClickListener(v -> {
            if (isEditing) return;
            if (cards.isEmpty()) return;
            currentIndex = (currentIndex - 1 + cards.size()) % cards.size();
            showingQuestion = true;
            updateCardDisplay();
        });

        // Navigate right (only in view mode)
        rightArrowButton.setOnClickListener(v -> {
            if (isEditing) return;
            if (cards.isEmpty()) return;
            currentIndex = (currentIndex + 1) % cards.size();
            showingQuestion = true;
            updateCardDisplay();
        });

        // Add card for this course (admin only)
        addCardButton.setOnClickListener(v -> {
            if (courseId == -1) {
                Toast.makeText(this,
                        "Course not loaded. Go back and re-select the course.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(AddFlashcardActivity.addFlashcardIntentFactory(this, courseId));
        });

        // DELETE CARD (admin only)
        deleteCardButton.setOnClickListener(v -> {
            if (cards.isEmpty()) {
                Toast.makeText(this, "No card to delete", Toast.LENGTH_SHORT).show();
                return;
            }

            Flashcard cardToDelete = cards.get(currentIndex);

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Delete Card")
                    .setMessage("Delete this card?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        GymLogDatabase.databaseWriteExecutor.execute(() -> {
                            GymLogDatabase db = GymLogDatabase.getDatabase(this);
                            db.flashcardDAO().delete(cardToDelete);

                            runOnUiThread(() -> {
                                if (currentIndex > 0) currentIndex--;
                                loadFlashcards();
                                Toast.makeText(this, "Card deleted", Toast.LENGTH_SHORT).show();
                            });
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // EDIT CARD (admin only)
        editCardButton.setOnClickListener(v -> {
            if (cards.isEmpty()) {
                Toast.makeText(this, "No card to edit", Toast.LENGTH_SHORT).show();
                return;
            }
            enterEditMode();
        });

        // SAVE CARD (admin only)
        saveCardButton.setOnClickListener(v -> {
            if (cards.isEmpty()) return;

            String newQ = editQuestionEditText.getText().toString().trim();
            String newA = editAnswerEditText.getText().toString().trim();

            if (newQ.isEmpty()) {
                Toast.makeText(this, "Question cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            if (newA.isEmpty()) {
                Toast.makeText(this, "Answer cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            Flashcard current = cards.get(currentIndex);
            current.setQuestion(newQ);
            current.setAnswer(newA);

            GymLogDatabase.databaseWriteExecutor.execute(() -> {
                GymLogDatabase db = GymLogDatabase.getDatabase(this);
                db.flashcardDAO().update(current);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Card saved", Toast.LENGTH_SHORT).show();
                    exitEditMode();
                    loadFlashcards();
                });
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFlashcards();
    }

    private void loadFlashcards() {
        if (courseId == -1) {
            cardTextView.setText("Invalid course.");
            positionTextView.setText("");
            return;
        }

        GymLogDatabase.databaseWriteExecutor.execute(() -> {
            GymLogDatabase db = GymLogDatabase.getDatabase(this);
            FlashcardDAO dao = db.flashcardDAO();
            List<Flashcard> results = dao.getFlashcardsForCourse(courseId);

            runOnUiThread(() -> {
                cards.clear();
                if (results != null) cards.addAll(results);

                if (currentIndex >= cards.size()) {
                    currentIndex = Math.max(0, cards.size() - 1);
                }

                showingQuestion = true;

                // if we were editing but list changed, exit edit mode safely
                if (isEditing) {
                    exitEditMode();
                }

                updateCardDisplay();
            });
        });
    }

    private void updateCardDisplay() {
        if (cards.isEmpty()) {
            cardTextView.setText("No flashcards yet.\n(Tap to flip) Use arrows to navigate.");
            positionTextView.setText("");
            leftArrowButton.setEnabled(false);
            rightArrowButton.setEnabled(false);
            return;
        }

        leftArrowButton.setEnabled(true);
        rightArrowButton.setEnabled(true);

        Flashcard current = cards.get(currentIndex);

        String text = showingQuestion ? current.getQuestion() : current.getAnswer();
        String prefix = showingQuestion ? "Q: " : "A: ";

        cardTextView.setText(prefix + (text == null ? "" : text));
        positionTextView.setText((currentIndex + 1) + " / " + cards.size() + "   (Tap to flip)");
    }

    private void enterEditMode() {
        isEditing = true;

        Flashcard current = cards.get(currentIndex);
        editQuestionEditText.setText(current.getQuestion() == null ? "" : current.getQuestion());
        editAnswerEditText.setText(current.getAnswer() == null ? "" : current.getAnswer());

        // show edit UI
        editQuestionEditText.setVisibility(View.VISIBLE);
        editAnswerEditText.setVisibility(View.VISIBLE);
        saveCardButton.setVisibility(View.VISIBLE);

        // hide view UI parts
        cardTextView.setVisibility(View.GONE);

        // hide other admin buttons while editing
        addCardButton.setEnabled(false);
        editCardButton.setEnabled(false);
        deleteCardButton.setEnabled(false);

        // prevent navigation during edit
        leftArrowButton.setEnabled(false);
        rightArrowButton.setEnabled(false);
    }

    private void exitEditMode() {
        isEditing = false;

        // hide edit UI
        editQuestionEditText.setVisibility(View.GONE);
        editAnswerEditText.setVisibility(View.GONE);
        saveCardButton.setVisibility(View.GONE);

        // show view UI
        cardTextView.setVisibility(View.VISIBLE);

        // restore admin buttons
        if (isAdmin) {
            addCardButton.setEnabled(true);
            editCardButton.setEnabled(true);
            deleteCardButton.setEnabled(true);
        }

        // restore navigation depending on list
        boolean hasCards = !cards.isEmpty();
        leftArrowButton.setEnabled(hasCards);
        rightArrowButton.setEnabled(hasCards);
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // If editing, just cancel edit and stay on screen
            if (isEditing) {
                exitEditMode();
                updateCardDisplay();
                return true;
            }
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
