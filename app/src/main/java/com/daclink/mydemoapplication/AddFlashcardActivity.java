package com.daclink.mydemoapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.daclink.mydemoapplication.Database.GymLogDatabase;
import com.daclink.mydemoapplication.Database.entities.Flashcard;

public class AddFlashcardActivity extends AppCompatActivity {

    private static final String EXTRA_COURSE_ID =
            "com.daclink.mydemoapplication.EXTRA_COURSE_ID";

    public static Intent addFlashcardIntentFactory(Context context, int courseId) {
        Intent intent = new Intent(context, AddFlashcardActivity.class);
        intent.putExtra(EXTRA_COURSE_ID, courseId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_flashcard);

        int courseId = getIntent().getIntExtra(EXTRA_COURSE_ID, -1);
        if (courseId == -1) {
            finish();
            return;
        }

        EditText questionEditText = findViewById(R.id.questionEditText);
        EditText answerEditText = findViewById(R.id.answerEditText);
        Button saveButton = findViewById(R.id.saveFlashcardButton);

        saveButton.setOnClickListener(v -> {
            String question = questionEditText.getText().toString().trim();
            String answer = answerEditText.getText().toString().trim();

            if (question.isEmpty() || answer.isEmpty()) {
                Toast.makeText(this, "Both fields required", Toast.LENGTH_SHORT).show();
                return;
            }

            GymLogDatabase.databaseWriteExecutor.execute(() -> {
                GymLogDatabase db = GymLogDatabase.getDatabase(this);
                db.flashcardDAO().insert(
                        new Flashcard(courseId, question, answer)
                );
                runOnUiThread(this::finish);
            });
        });
    }
}
