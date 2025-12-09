package com.daclink.mydemoapplication;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;

import com.daclink.mydemoapplication.Database.GymLogRepository;
import com.daclink.mydemoapplication.Database.entities.GymLog;
import com.daclink.mydemoapplication.Database.entities.User;
import com.daclink.mydemoapplication.databinding.ActivityMainBinding;

import java.util.ArrayList;

/*
 * Author: France Zhang
 * Created on: 12/02/2025
 * Description: MainActivity class
 */

public class MainActivity extends AppCompatActivity {

    // Keys for passing and saving user ID
    public static final String MAIN_ACTIVITY_USER_ID =
            "com.daclink.mydemoapplication.MAIN_ACTIVITY_USER_ID";

    public static final String SHARED_PREFERENCE_USERID_KEY =
            "com.daclink.mydemoapplication.SHARED_PREFERENCE_USERID_KEY";

    public static final String SHARED_PREFERENCE_USERID_VALUE =
            "com.daclink.mydemoapplication.SHARED_PREFERENCE_USERID_VALUE";

    public static final String SAVED_INSTANCE_STATE_USERID_KEY =
            "com.daclink.mydemoapplication.SAVED_INSTANCE_STATE_USERID_KEY";

    private static final int LOGGED_OUT = -1;

    private ActivityMainBinding binding;
    private GymLogRepository repository;

    public static final String TAG = "DAC_GYMLOG";

    String mExercise = "";
    double mWeight = 0.0;
    int mReps = 0;

    private int loggedInUserId = LOGGED_OUT;
    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // IMPORTANT FIX: Repository must be created BEFORE loginUser()
        repository = GymLogRepository.getRepository(getApplication());

        // Determine who is logged in
        loginUser(savedInstanceState);

        // If still not logged in, go to LoginActivity
        if (loggedInUserId == LOGGED_OUT) {
            startActivity(LoginActivity.loginIntentFactory(getApplicationContext()));
        }

        binding.logDisplayTextView.setMovementMethod(new ScrollingMovementMethod());
        updateDisplay();

        binding.logButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getInformationFromDisplay();
                insertGymLogRecord();
                updateDisplay();
            }
        });
    }

    /**
     * Matches the professor’s logic:
     *   1. Try SharedPreferences
     *   2. Try savedInstanceState
     *   3. Try Intent
     * Then observe the User.
     */
    private void loginUser(Bundle savedInstanceState) {
        // 1. SharedPreferences
        SharedPreferences sharedPreferences =
                getSharedPreferences(SHARED_PREFERENCE_USERID_KEY, Context.MODE_PRIVATE);

        if (sharedPreferences.contains(SHARED_PREFERENCE_USERID_VALUE)) {
            loggedInUserId = sharedPreferences.getInt(
                    SHARED_PREFERENCE_USERID_VALUE,
                    LOGGED_OUT
            );
        }

        // 2. savedInstanceState
        if (loggedInUserId == LOGGED_OUT &&
                savedInstanceState != null &&
                savedInstanceState.containsKey(SAVED_INSTANCE_STATE_USERID_KEY)) {

            loggedInUserId = savedInstanceState.getInt(
                    SAVED_INSTANCE_STATE_USERID_KEY,
                    LOGGED_OUT
            );
        }

        // 3. Intent extra (from LoginActivity)
        if (loggedInUserId == LOGGED_OUT) {
            loggedInUserId = getIntent().getIntExtra(
                    MAIN_ACTIVITY_USER_ID,
                    LOGGED_OUT
            );
        }

        // If STILL not logged in → stop; onCreate will redirect
        if (loggedInUserId == LOGGED_OUT) {
            return;
        }

        // Observe the user in the DB
        attachUserObserver();
    }

    // Called by loginUser() to fully load the User object
    private void attachUserObserver() {
        LiveData<User> userObserver = repository.getUserByUserId(loggedInUserId);
        userObserver.observe(this, userFromDb -> {
            this.user = userFromDb;
            if (this.user != null) {
                invalidateOptionsMenu();
            } else {
                logout();
            }
        });
    }

    // Save user ID so Android restores it after rotation
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(SAVED_INSTANCE_STATE_USERID_KEY, loggedInUserId);

        SharedPreferences sharedPreferences =
                getSharedPreferences(SHARED_PREFERENCE_USERID_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor sharedPrefEditor = sharedPreferences.edit();
        sharedPrefEditor.putInt(SHARED_PREFERENCE_USERID_VALUE, loggedInUserId);
        sharedPrefEditor.apply();
    }

    // Menu handling (Logout)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.logout_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.logoutMenuItem);

        if (user == null) {
            item.setVisible(false);
            return false;
        }

        item.setVisible(true);
        item.setTitle(user.getUsername());
        item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(@NonNull MenuItem menuItem) {
                showLogoutDialog();
                return true;
            }
        });

        return true;
    }

    private void showLogoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        builder.setMessage("Logout?");
        builder.setPositiveButton("Logout", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                logout();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void logout() {
        SharedPreferences sharedPreferences =
                getSharedPreferences(SHARED_PREFERENCE_USERID_KEY, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(SHARED_PREFERENCE_USERID_VALUE, LOGGED_OUT);
        editor.apply();

        loggedInUserId = LOGGED_OUT;
        user = null;

        startActivity(LoginActivity.loginIntentFactory(getApplicationContext()));
        finish();
    }

    // Factory to launch MainActivity with a userId
    public static Intent mainActivityIntentFactory(Context context, int userId) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(MAIN_ACTIVITY_USER_ID, userId);
        return intent;
    }

    // GymLog insertion
    private void insertGymLogRecord() {
        if (mExercise.isEmpty()) {
            return;
        }
        GymLog log = new GymLog(mExercise, mWeight, mReps, loggedInUserId);
        repository.insertGymLog(log);
    }

    // Display log list
    private void updateDisplay() {
        ArrayList<GymLog> allLogs = repository.getAllLogs();

        if (allLogs == null || allLogs.isEmpty()) {
            binding.logDisplayTextView.setText(
                    R.string.nothing_to_show_time_to_hit_the_gym
            );
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (GymLog log : allLogs) {
            sb.append(log.toString()).append("\n");
        }
        binding.logDisplayTextView.setText(sb.toString());
    }

    // Read the EditText fields
    private void getInformationFromDisplay() {
        mExercise = binding.exerciseInputEditText.getText().toString();

        try {
            mWeight = Double.parseDouble(
                    binding.weightInputEditText.getText().toString()
            );
        } catch (NumberFormatException e) {
            mWeight = 0.0;
            Log.d(TAG, "Invalid weight value");
        }

        try {
            mReps = Integer.parseInt(
                    binding.repInputEditText.getText().toString()
            );
        } catch (NumberFormatException e) {
            mReps = 0;
            Log.d(TAG, "Invalid reps value");
        }
    }
}
