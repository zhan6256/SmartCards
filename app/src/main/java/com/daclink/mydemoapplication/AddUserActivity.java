package com.daclink.mydemoapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.daclink.mydemoapplication.Database.GymLogDatabase;
import com.daclink.mydemoapplication.Database.UserDAO;
import com.daclink.mydemoapplication.Database.entities.User;

public class AddUserActivity extends AppCompatActivity {

    public static Intent addUserIntentFactory(Context context) {
        return new Intent(context, AddUserActivity.class);
    }

    private EditText usernameEditText;
    private EditText passwordEditText;
    private CheckBox adminCheckBox;
    private Button saveButton;
    private Button cancelButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_user);

        usernameEditText = findViewById(R.id.newUsernameEditText);
        passwordEditText = findViewById(R.id.newPasswordEditText);
        adminCheckBox = findViewById(R.id.newAdminCheckBox);
        saveButton = findViewById(R.id.saveUserButton);
        cancelButton = findViewById(R.id.cancelUserButton);

        cancelButton.setOnClickListener(v -> finish());

        saveButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            boolean isAdmin = adminCheckBox.isChecked();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Username and password required.", Toast.LENGTH_SHORT).show();
                return;
            }

            GymLogDatabase.databaseWriteExecutor.execute(() -> {
                GymLogDatabase db = GymLogDatabase.getDatabase(this);
                UserDAO userDAO = db.userDAO();

                // Prevent duplicates (you already have getUserByUsernameSync in your seed code)
                User existing = userDAO.getUserByUsernameSync(username);
                if (existing != null) {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Username already exists.", Toast.LENGTH_SHORT).show());
                    return;
                }

                User newUser = new User(username, password);
                newUser.setAdmin(isAdmin);
                userDAO.insert(newUser);

                runOnUiThread(() -> {
                    Toast.makeText(this, "User added!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            });
        });
    }
}
