package com.daclink.mydemoapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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
 * Description: DashboardActivity
 */
public class DashboardActivity extends AppCompatActivity {

    public static final String DASHBOARD_USER_ID =
            "com.daclink.mydemoapplication.DASHBOARD_USER_ID";

    private GymLogRepository repository;
    private int loggedInUserId;

    private TextView usernameTextView;
    private TextView adminControlsLabelTextView;
        private LinearLayout courseContainer;
    private LinearLayout adminControlsGroup;

    // Admin controls
    private Button userListButton;
    private Button addUserButton;

    private Button editCourseButton;
    private Button deleteCourseButton;
    private Button logoutButton;
    private Button addCourseButton;

    private Course selectedCourse = null;

    private User currentUser = null; // used for menu title (admin1, testuser1)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // ----- Get logged-in user ID -----
        loggedInUserId = getIntent().getIntExtra(DASHBOARD_USER_ID, -1);
        if (loggedInUserId == -1) {
            startActivity(LoginActivity.loginIntentFactory(this));
            finish();
            return;
        }

        // ----- Views -----
        usernameTextView = findViewById(R.id.usernameTextView);
        courseContainer = findViewById(R.id.courseContainer);
        adminControlsGroup = findViewById(R.id.adminControlsGroup);
        adminControlsLabelTextView = findViewById(R.id.adminControlsLabelTextView);
        // Admin buttons (must exist in your XML)
        userListButton = findViewById(R.id.userListButton);
        addUserButton = findViewById(R.id.addUserButton);
        addCourseButton = findViewById(R.id.addCourseButton);


        editCourseButton = findViewById(R.id.editCourseButton);
        deleteCourseButton = findViewById(R.id.deleteCourseButton);
        logoutButton = findViewById(R.id.logoutButton);

        repository = GymLogRepository.getRepository(getApplication());

        // ----- Load User -----
        LiveData<User> userObserver = repository.getUserByUserId(loggedInUserId);
        userObserver.observe(this, user -> {
            if (user == null) {
                logout();
                return;
            }

            currentUser = user; // save for menu title
            usernameTextView.setText("Username: " + user.getUsername());

            // Admin-only controls
            boolean isAdmin = user.isAdmin();
            adminControlsGroup.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
            adminControlsLabelTextView.setVisibility(isAdmin ? View.VISIBLE : View.GONE);

            userListButton.setEnabled(isAdmin);
            addUserButton.setEnabled(isAdmin);
            addCourseButton.setEnabled(isAdmin);


            // refresh the 3-dots menu title
            invalidateOptionsMenu();
        });

        // Wire admin buttons
        addUserButton.setOnClickListener(v ->
                startActivity(AddUserActivity.addUserIntentFactory(this))
        );
        addCourseButton.setOnClickListener(v -> showAddCourseDialog());


        userListButton.setOnClickListener(v ->
                startActivity(UserListActivity.userListIntentFactory(this, loggedInUserId))
        );


        // ----- Load courses -----
        loadCourses();

        // ----- Edit Course -----
        editCourseButton.setOnClickListener(v -> {
            if (selectedCourse == null) {
                Toast.makeText(this, "Select a course first (long-press)", Toast.LENGTH_SHORT).show();
                return;
            }
            showEditCourseDialog(selectedCourse);
        });

        // ----- Delete Course -----
        deleteCourseButton.setOnClickListener(v -> {
            if (selectedCourse == null) {
                Toast.makeText(this, "Select a course first (long-press)", Toast.LENGTH_SHORT).show();
                return;
            }
            confirmDeleteCourse(selectedCourse);
        });

        // ----- Logout Button (keep for now) -----
        logoutButton.setOnClickListener(v -> logout());

    }

    // =========================================================
    // 3-dots menu (Logout for everyone)
    // =========================================================
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.logout_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.logoutMenuItem);

        // If user not loaded yet, just show "Logout"
        if (currentUser == null) {
            item.setTitle("Logout");
            return true;
        }

        // Show username like GymLog (tap it -> logout dialog)
        item.setTitle(currentUser.getUsername());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.logoutMenuItem) {
            showLogoutDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setMessage("Logout?")
                .setPositiveButton("Logout", (dialog, which) -> logout())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // =========================================================
    // Load courses and create buttons
    // =========================================================
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

                    // Select course (long press)
                    courseButton.setOnLongClickListener(v -> {
                        selectedCourse = course;
                        Toast.makeText(
                                this,
                                "Selected: " + course.getCourseName(),
                                Toast.LENGTH_SHORT
                        ).show();
                        return true;
                    });

                    // Tap → Flashcards (pass courseId + name)
                    courseButton.setOnClickListener(v -> {
                        Intent intent = FlashcardsActivity.flashcardsIntentFactory(
                                this,
                                course.getCourseId(),
                                course.getCourseName()
                        );
                        startActivity(intent);
                    });

                    courseContainer.addView(courseButton);
                }
            });
        });
    }

    // =========================================================
    // Edit course dialog
    // =========================================================
    private void showEditCourseDialog(Course course) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_course, null);

        TextView nameEditText = dialogView.findViewById(R.id.editCourseName);
        TextView descEditText = dialogView.findViewById(R.id.editCourseDescription);

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
    private void showAddCourseDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_course, null);

        TextView nameEditText = dialogView.findViewById(R.id.editCourseName);
        TextView descEditText = dialogView.findViewById(R.id.editCourseDescription);

        nameEditText.setText("");
        descEditText.setText("");

        new AlertDialog.Builder(this)
                .setTitle("Add Course")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = nameEditText.getText().toString().trim();
                    String desc = descEditText.getText().toString().trim();

                    if (name.isEmpty()) {
                        Toast.makeText(this, "Course name cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    GymLogDatabase.databaseWriteExecutor.execute(() -> {
                        GymLogDatabase db = GymLogDatabase.getDatabase(this);
                        CourseDAO dao = db.getCourseDAO();

                        // matches your Course constructor
                        Course newCourse = new Course(name, desc, loggedInUserId);
                        dao.insert(newCourse);

                        runOnUiThread(this::loadCourses);
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }



    // =========================================================
    // Delete course confirmation
    // =========================================================
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

    // =========================================================
    // Logout
    // =========================================================
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

    // =========================================================
    // Intent factory
    // =========================================================
    public static Intent dashboardIntentFactory(Context context, int userId) {
        Intent intent = new Intent(context, DashboardActivity.class);
        intent.putExtra(DASHBOARD_USER_ID, userId);
        return intent;
    }
}
