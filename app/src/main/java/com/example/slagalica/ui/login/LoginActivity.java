package com.example.slagalica.ui.login;

import android.os.Bundle;
import android.content.Intent;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.slagalica.R;
import com.example.slagalica.ui.auth.AuthViewModel;
import com.example.slagalica.ui.home.HomeActivity;
import com.example.slagalica.ui.register.RegisterActivity;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        TextInputEditText etEmail = findViewById(R.id.etEmail);
        TextInputEditText etPassword = findViewById(R.id.etPassword);
        Button btnSubmitLogin = findViewById(R.id.btnSubmitLogin);
        Button btnGoToRegister = findViewById(R.id.btnGoToRegister);

        authViewModel.getErrorMessage().observe(this, msg -> {
            if (msg != null) {
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            }
        });

        authViewModel.getLoginSuccess().observe(this, success -> {
            if (success != null && success) {
                Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                startActivity(intent);
                finish();
            }
        });

        authViewModel.getIsLoading().observe(this, isLoading -> {
            btnSubmitLogin.setEnabled(isLoading == null || !isLoading);
        });

        btnSubmitLogin.setOnClickListener(v -> {
            String emailOrUsername = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

            authViewModel.login(emailOrUsername, password);
        });

        if(btnGoToRegister != null) {
            btnGoToRegister.setOnClickListener(v -> {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
                finish();
            });
        }
    }
}
