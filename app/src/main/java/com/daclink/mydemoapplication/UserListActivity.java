package com.daclink.mydemoapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;

import com.daclink.mydemoapplication.Database.GymLogRepository;
import com.daclink.mydemoapplication.Database.entities.User;

import java.util.List;

public class UserListActivity extends AppCompatActivity {

    private static final String EXTRA_LOGGED_IN_USER_ID =
            "com.daclink.mydemoapplication.EXTRA_LOGGED_IN_USER_ID";

    private GymLogRepository repository;
    private int loggedInUserId;

    private LinearLayout userListContainer;

    public static Intent userListIntentFactory(Context context, int loggedInUserId) {
        Intent intent = new Intent(context, UserListActivity.class);
        intent.putExtra(EXTRA_LOGGED_IN_USER_ID, loggedInUserId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        loggedInUserId = getIntent().getIntExtra(EXTRA_LOGGED_IN_USER_ID, -1);
        if (loggedInUserId == -1) {
            startActivity(LoginActivity.loginIntentFactory(this));
            finish();
            return;
        }

        userListContainer = findViewById(R.id.userListContainer);
        repository = GymLogRepository.getRepository(getApplication());

        // Only admin can be here (simple check)
        repository.getUserByUserId(loggedInUserId).observe(this, me -> {
            if (me == null || !me.isAdmin()) {
                Toast.makeText(this, "Admin only.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            loadUsers();
        });
    }

    private void loadUsers() {
        LiveData<List<User>> usersLive = repository.getAllUsers();
        usersLive.observe(this, users -> {
            userListContainer.removeAllViews();

            for (User u : users) {
                View row = getLayoutInflater().inflate(R.layout.user_row, userListContainer, false);

                TextView usernameTv = row.findViewById(R.id.userRowUsernameTextView);
                Button deleteBtn = row.findViewById(R.id.userRowDeleteButton);

                usernameTv.setText(u.getUsername());

                // Prevent deleting admin1 (recommended)
                boolean isAdmin1 = "admin1".equalsIgnoreCase(u.getUsername());
                if (isAdmin1) {
                    deleteBtn.setEnabled(false);
                    deleteBtn.setAlpha(0.4f);
                } else {
                    deleteBtn.setOnClickListener(v -> confirmDeleteUser(u));
                }

                userListContainer.addView(row);
            }
        });
    }

    private void confirmDeleteUser(User user) {
        new AlertDialog.Builder(this)
                .setTitle("Delete User")
                .setMessage("Delete user \"" + user.getUsername() + "\"?")
                .setPositiveButton("Delete", (d, which) -> {
                    repository.deleteUser(user);
                    Toast.makeText(this, "Deleted: " + user.getUsername(), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
