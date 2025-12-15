package com.daclink.mydemoapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class FlashcardsActivity extends AppCompatActivity {

    private static final String EXTRA_COURSE_NAME =
            "com.daclink.mydemoapplication.EXTRA_COURSE_NAME";

    public static Intent flashcardsIntentFactory(Context context, String courseName) {
        Intent intent = new Intent(context, FlashcardsActivity.class);
        intent.putExtra(EXTRA_COURSE_NAME, courseName);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcards);
        // Show the back arrow in the top-left
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }


        String courseName = getIntent().getStringExtra(EXTRA_COURSE_NAME);

        TextView titleTextView = findViewById(R.id.flashcardsTitleTextView);
        titleTextView.setText("Flashcards for: " + courseName);
    }
    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // go back to DashboardActivity
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
