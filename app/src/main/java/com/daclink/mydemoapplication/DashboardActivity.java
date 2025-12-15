package com.daclink.mydemoapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;

import com.daclink.mydemoapplication.Database.CourseDAO;
import com.daclink.mydemoapplication.Database.GymLogDatabase;
import com.daclink.mydemoapplication.Database.GymLogRepository;
import com.daclink.mydemoapplication.Database.entities.Course;
import com.daclink.mydemoapplication.Database.entities.User;

import java.util.List;

/*
 * Author: France Zhang
 * Description: DashboardActivity (no toolbar menu)
 */

public class DashboardActivity extends AppCompatActivity {

    public static final String DASHBOARD_USER_ID =
            "com.daclink.mydemoapplication.DASHBOARD_USER_ID";

    private GymLogRepository repository;
    private int loggedInUserId;

    private TextView usernameTextView;
    private LinearLayout courseContainer;
    private LinearLayout adminControlsGroup;

    private Button editCourseButton;
    private Button deleteCourseButton;
    private Button logoutButton;

    private Course selectedCourse = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Get user id
        loggedInUserId = getIntent().getIntExtra(DASHBOARD_USER_ID, -1);
        if (loggedInUserId == -1) {
            startActivity(LoginActivity.loginIntentFactory(this));
            finish();
            return;
        }

        // Views
        usernameTextView = findViewById(R.id.usernameTextView);
        courseContainer = findViewById(R.id.courseContainer);
        adminControlsGroup = findViewById(R.id.adminControlsGroup);
        editCourseButton = findViewById(R.id.editCourseButton);
        deleteCourseButton = findViewById(R.id.deleteCourseButton);
        logoutButton = findViewById(R.id.logoutButton);

        repository = GymLogRepository.getRepository(getApplication());

        // Load user
        LiveData<User> userObserver = repository.getUserByUserId(loggedInUserId);
        userObserver.observe(this, user -> {
            if (user == null) {
                logout();
                return;
            }

            usernameTextView.setText("Username: " + user.getUsername());

            // Admin-only controls (per your spec: admin1 is admin)
            boolean isAdmin = "admin1".equalsIgnoreCase(user.getUsername());
            if (!isAdmin) {
                adminControlsGroup.setVisibility(View.GONE);
            } else {
                adminControlsGroup.setVisibility(View.VISIBLE);
            }
        });

        // Load courses
        loadCourses();

        // Edit course
        editCourseButton.setOnClickListener(v -> {
            if (selectedCourse == null) {
                Toast.makeText(this, "Select a course first", Toast.LENGTH_SHORT).show();
                return;
            }
            showEditCourseDialog(selectedCourse);
        });

        // Delete course
        deleteCourseButton.setOnClickListener(v -> {
            if (selectedCourse == null) {
                Toast.makeText(this, "Select a course first", Toast.LENGTH_SHORT).show();
                return;
            }
            confirmDeleteCourse(selectedCourse);
        });

        // Logout
        logoutButton.setOnClickListener(v -> logout());
    }

    private void loadCourses() {
        GymLogDatabase.databaseWriteExecutor.execute(() -> {
            GymLogDatabase db = GymLogDatabase.getDatabase(this);
            CourseDAO courseDAO = db.getCourseDAO();
            List<Course> courses = courseDAO.getAllCourses();

            runOnUiThread(() -> {
                courseContainer.removeAllViews();
                selectedCourse = null;

                for (Course course : courses) {
                    Button courseButton = new Button(this);
                    courseButton.setText(course.getCourseName());
                    courseButton.setAllCaps(false);

                    courseButton.setOnClickListener(v -> {
                        selectedCourse = course;
                        Toast.makeText(
                                this,
                                "Selected: " + course.getCourseName(),
                                Toast.LENGTH_SHORT
                        ).show();
                    });

                    courseContainer.addView(courseButton);
                }
            });
        });
    }

    private void showEditCourseDialog(Course course) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_course, null);

        EditText nameEditText = dialogView.findViewById(R.id.editCourseName);
        EditText descEditText = dialogView.findViewById(R.id.editCourseDescription);

        nameEditText.setText(course.getCourseName());
        descEditText.setText(course.getCourseDescription());

        new AlertDialog.Builder(this)
                .setTitle("Edit Course")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = nameEditText.getText().toString().trim();
                    String newDesc = descEditText.getText().toString().trim();

                    if (newName.isEmpty()) {
                        Toast.makeText(this, "Course name cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    GymLogDatabase.databaseWriteExecutor.execute(() -> {
                        GymLogDatabase db = GymLogDatabase.getDatabase(this);
                        CourseDAO dao = db.getCourseDAO();

                        course.setCourseName(newName);
                        course.setCourseDescription(newDesc);

                        dao.update(course);

                        runOnUiThread(this::loadCourses);
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDeleteCourse(Course course) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Course")
                .setMessage("Delete \"" + course.getCourseName() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    GymLogDatabase.databaseWriteExecutor.execute(() -> {
                        GymLogDatabase db = GymLogDatabase.getDatabase(this);
                        CourseDAO dao = db.getCourseDAO();

                        dao.delete(course);

                        runOnUiThread(this::loadCourses);
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logout() {
        SharedPreferences sharedPreferences =
                getSharedPreferences(
                        MainActivity.SHARED_PREFERENCE_USERID_KEY,
                        Context.MODE_PRIVATE
                );

        sharedPreferences.edit()
                .putInt(MainActivity.SHARED_PREFERENCE_USERID_VALUE, -1)
                .apply();

        startActivity(LoginActivity.loginIntentFactory(this));
        finish();
    }

    public static Intent dashboardIntentFactory(Context context, int userId) {
        Intent intent = new Intent(context, DashboardActivity.class);
        intent.putExtra(DASHBOARD_USER_ID, userId);
        return intent;
    }
}
