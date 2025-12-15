package com.daclink.mydemoapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;

import com.daclink.mydemoapplication.Database.GymLogRepository;
import com.daclink.mydemoapplication.Database.entities.User;
import com.daclink.mydemoapplication.databinding.ActivityLoginBinding;

/*
 * Author: France Zhang
 * Created on: 12/02/2025
 * Description: LoginActivity class
 */

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;

    private GymLogRepository repository;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = GymLogRepository.getRepository(getApplication());

        binding.loginButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                verifyUser();
            }
        });
    }
    private void verifyUser(){
        String username = binding.userNameLoginEditText.getText().toString();

        if(username.isEmpty()){
            toastMaker("username should not be blank");
            return;
        }
        LiveData<User> userObserver = repository.getUserByUserName(username);
        userObserver.observe(this, user ->{
            if(user != null){
                String password = binding.passwordLoginEditText.getText().toString();
                if(password.equals(user.getPassword())){
                    startActivity(DashboardActivity.dashboardIntentFactory(
                            getApplicationContext(),
                            user.getId()
                    ));
                    finish();

                }else{
                    toastMaker("Invalid password" +
                            "");
                    binding.passwordLoginEditText.setSelection(0);

                }
            }else {
                toastMaker (String.format("%s is not a valid username.", username));
                binding.userNameLoginEditText.setSelection(0);
            }

        });
    }

    private void toastMaker(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    static Intent loginIntentFactory(Context context){
        return new Intent(context, LoginActivity.class);
    }

}