package com.example.slagalica.ui.register;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import android.content.Intent;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.textfield.TextInputEditText;

import androidx.appcompat.app.AppCompatActivity;

import com.example.slagalica.R;
import com.example.slagalica.ui.auth.AuthViewModel;
import com.example.slagalica.ui.login.LoginActivity;

public class RegisterActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        TextInputEditText etEmail = findViewById(R.id.etEmailRegister);
        TextInputEditText etUsername = findViewById(R.id.etUsername);
        TextInputEditText etRegion = findViewById(R.id.etRegion);
        TextInputEditText etPassword = findViewById(R.id.etPasswordRegister);
        TextInputEditText etRepeatPassword = findViewById(R.id.etRepeatPassword);

        Button btnSubmitRegister = findViewById(R.id.btnSubmitRegister);
        
        authViewModel.getErrorMessage().observe(this, msg -> {
            if (msg != null) {
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            }
        });

        authViewModel.getRegisterSuccess().observe(this, success -> {
            if (success != null && success) {
                Toast.makeText(this, R.string.register_toast_message, Toast.LENGTH_LONG).show();
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        authViewModel.getIsLoading().observe(this, isLoading -> {
            btnSubmitRegister.setEnabled(isLoading == null || !isLoading);
        });

        btnSubmitRegister.setOnClickListener(v -> {
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
            String region = etRegion.getText() != null ? etRegion.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
            String repeatPassword = etRepeatPassword.getText() != null ? etRepeatPassword.getText().toString().trim() : "";
            
            authViewModel.register(email, username, region, password, repeatPassword);
        });

        Button btnGoToLogin = findViewById(R.id.btnGoToLogin);
        btnGoToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
