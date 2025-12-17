package com.daclink.mydemoapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.daclink.mydemoapplication.Database.FlashcardDAO;
import com.daclink.mydemoapplication.Database.GymLogDatabase;
import com.daclink.mydemoapplication.Database.entities.Flashcard;

public class AddFlashcardActivity extends AppCompatActivity {

    private static final String EXTRA_COURSE_ID =
            "com.daclink.mydemoapplication.EXTRA_COURSE_ID";

    private int courseId;

    private EditText questionEditText;
    private EditText answerEditText;
    private Button saveButton;
    private Button cancelButton;

    public static Intent addFlashcardIntentFactory(Context context, int courseId) {
        Intent intent = new Intent(context, AddFlashcardActivity.class);
        intent.putExtra(EXTRA_COURSE_ID, courseId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_flashcard);

        // Get courseId
        courseId = getIntent().getIntExtra(EXTRA_COURSE_ID, -1);
        if (courseId == -1) {
            Toast.makeText(this, "Missing course id", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Views (these IDs must exist in activity_add_flashcard.xml)
        questionEditText = findViewById(R.id.questionEditText);
        answerEditText = findViewById(R.id.answerEditText);
        saveButton = findViewById(R.id.saveFlashcardButton);
        cancelButton = findViewById(R.id.cancelFlashcardButton);

        cancelButton.setOnClickListener(v -> finish());

        saveButton.setOnClickListener(v -> {
            String q = questionEditText.getText().toString().trim();
            String a = answerEditText.getText().toString().trim();

            if (q.isEmpty()) {
                Toast.makeText(this, "Question cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            if (a.isEmpty()) {
                Toast.makeText(this, "Answer cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            GymLogDatabase.databaseWriteExecutor.execute(() -> {
                GymLogDatabase db = GymLogDatabase.getDatabase(this);
                FlashcardDAO flashcardDAO = db.flashcardDAO();

                flashcardDAO.insert(new Flashcard(courseId, q, a));

                runOnUiThread(() -> {
                    Toast.makeText(this, "Card added", Toast.LENGTH_SHORT).show();
                    finish(); // go back to FlashcardsActivity
                });
            });
        });
    }
}
