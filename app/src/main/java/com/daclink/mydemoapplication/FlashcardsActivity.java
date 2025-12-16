package com.daclink.mydemoapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
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

    public static Intent flashcardsIntentFactory(Context context, int courseId, String courseName) {
        Intent intent = new Intent(context, FlashcardsActivity.class);
        intent.putExtra(EXTRA_COURSE_ID, courseId);
        intent.putExtra(EXTRA_COURSE_NAME, courseName);
        return intent;
    }

    private int courseId = -1;
    private String courseName = "";

    private TextView titleTextView;
    private TextView cardTextView;
    private TextView positionTextView;
    private Button leftArrowButton;
    private Button rightArrowButton;
    private Button addCardButton;

    private final ArrayList<Flashcard> flashcards = new ArrayList<>();
    private int index = 0;
    private boolean showingQuestion = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcards);

        // Show the back arrow in the top-left
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        courseId = getIntent().getIntExtra(EXTRA_COURSE_ID, -1);
        courseName = getIntent().getStringExtra(EXTRA_COURSE_NAME);

        if (courseId == -1) {
            Toast.makeText(this, "Missing courseId.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Views (IDs must exist in activity_flashcards.xml)
        titleTextView = findViewById(R.id.flashcardsTitleTextView);
        cardTextView = findViewById(R.id.cardTextView);
        positionTextView = findViewById(R.id.positionTextView);
        leftArrowButton = findViewById(R.id.leftArrowButton);
        rightArrowButton = findViewById(R.id.rightArrowButton);
        addCardButton = findViewById(R.id.addCardButton);

        titleTextView.setText("Flashcards for: " + courseName);

        // Tap card to flip
        cardTextView.setOnClickListener(v -> {
            if (flashcards.isEmpty()) return;
            showingQuestion = !showingQuestion;
            render();
        });

        // Left / Right navigation
        leftArrowButton.setOnClickListener(v -> {
            if (flashcards.isEmpty()) return;
            index = (index - 1 + flashcards.size()) % flashcards.size();
            showingQuestion = true;
            render();
        });

        rightArrowButton.setOnClickListener(v -> {
            if (flashcards.isEmpty()) return;
            index = (index + 1) % flashcards.size();
            showingQuestion = true;
            render();
        });

        // Add card
        addCardButton.setOnClickListener(v ->
                startActivity(AddFlashcardActivity.addFlashcardIntentFactory(this, courseId))
        );

        loadFlashcards();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // refresh after returning from AddFlashcardActivity
        loadFlashcards();
    }

    private void loadFlashcards() {
        GymLogDatabase.databaseWriteExecutor.execute(() -> {
            GymLogDatabase db = GymLogDatabase.getDatabase(this);
            FlashcardDAO dao = db.flashcardDAO();

            List<Flashcard> result = dao.getFlashcardsForCourse(courseId);

            runOnUiThread(() -> {
                flashcards.clear();
                if (result != null) flashcards.addAll(result);

                if (flashcards.isEmpty()) {
                    cardTextView.setText("(No flashcards yet. Tap Add Card.)");
                    positionTextView.setText("");
                    leftArrowButton.setEnabled(false);
                    rightArrowButton.setEnabled(false);
                } else {
                    leftArrowButton.setEnabled(true);
                    rightArrowButton.setEnabled(true);
                    index = 0;
                    showingQuestion = true;
                    render();
                }
            });
        });
    }

    private void render() {
        Flashcard current = flashcards.get(index);

        // Change these getters if your entity uses different names
        String front = current.getQuestion();
        String back = current.getAnswer();

        cardTextView.setText(showingQuestion ? front : back);
        positionTextView.setText((index + 1) + " / " + flashcards.size());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // go back to DashboardActivity
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
