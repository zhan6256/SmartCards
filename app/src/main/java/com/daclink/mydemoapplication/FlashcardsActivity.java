package com.daclink.mydemoapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.daclink.mydemoapplication.Database.FlashcardDAO;
import com.daclink.mydemoapplication.Database.GymLogDatabase;
import com.daclink.mydemoapplication.Database.entities.Flashcard;

import java.util.ArrayList;
import java.util.List;

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

    private Button addCardButton;
    private Button deleteCardButton;   // ✅ NEW
    private Button leftArrowButton;
    private Button rightArrowButton;

    private final List<Flashcard> cards = new ArrayList<>();
    private int currentIndex = 0;
    private boolean showingQuestion = true;

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

        // These IDs MUST match your activity_flashcards.xml
        titleTextView = findViewById(R.id.flashcardsTitleTextView);
        positionTextView = findViewById(R.id.positionTextView);
        cardTextView = findViewById(R.id.cardTextView);

        addCardButton = findViewById(R.id.addCardButton);
        deleteCardButton = findViewById(R.id.deleteCardButton); // ✅ NEW
        leftArrowButton = findViewById(R.id.leftArrowButton);
        rightArrowButton = findViewById(R.id.rightArrowButton);

        titleTextView.setText("Flashcards for: " + courseName);

        // ADMIN-ONLY: hide ADD/DELETE buttons for non-admin users
        SharedPreferences prefs =
                getSharedPreferences(MainActivity.SHARED_PREFERENCE_USERID_KEY, MODE_PRIVATE);
        boolean isAdmin = prefs.getBoolean("IS_ADMIN", false);
        if (!isAdmin) {
            addCardButton.setVisibility(View.GONE);      // invisible + no space
            deleteCardButton.setVisibility(View.GONE);   //  NEW
        }

        // Tap card to flip Q <-> A
        cardTextView.setOnClickListener(v -> {
            if (cards.isEmpty()) return;
            showingQuestion = !showingQuestion;
            updateCardDisplay();
        });

        // Navigate left
        leftArrowButton.setOnClickListener(v -> {
            if (cards.isEmpty()) return;
            currentIndex = (currentIndex - 1 + cards.size()) % cards.size();
            showingQuestion = true;
            updateCardDisplay();
        });

        // Navigate right
        rightArrowButton.setOnClickListener(v -> {
            if (cards.isEmpty()) return;
            currentIndex = (currentIndex + 1) % cards.size();
            showingQuestion = true;
            updateCardDisplay();
        });

        // Add card for this course (admin only - button hidden for non-admin)
        addCardButton.setOnClickListener(v -> {
            if (courseId == -1) {
                Toast.makeText(this,
                        "Course not loaded. Go back and re-select the course.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(AddFlashcardActivity.addFlashcardIntentFactory(this, courseId));
        });

        // DELETE CARD (admin only - button hidden for non-admin)
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
                                // keep index in range after delete
                                if (currentIndex > 0) currentIndex--;
                                loadFlashcards();
                                Toast.makeText(this, "Card deleted", Toast.LENGTH_SHORT).show();
                            });
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
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

                // If currentIndex is out of range after delete, fix it
                if (currentIndex >= cards.size()) {
                    currentIndex = Math.max(0, cards.size() - 1);
                }

                showingQuestion = true;
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

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
