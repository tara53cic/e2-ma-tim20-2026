package com.example.slagalica.ui.reset_password;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.slagalica.R;
import com.example.slagalica.ui.auth.AuthViewModel;
import com.google.android.material.textfield.TextInputEditText;

public class  ResetPasswordActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        TextInputEditText etOldPassword = findViewById(R.id.etOldPassword);
        TextInputEditText etNewPassword = findViewById(R.id.etNewPassword);
        TextInputEditText etRepeatNewPassword = findViewById(R.id.etRepeatNewPassword);
        Button btnSubmitResetPassword = findViewById(R.id.btnSubmitResetPassword);

        authViewModel.getErrorMessage().observe(this, msg -> {
            if (msg != null) {
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            }
        });

        authViewModel.getPasswordResetSuccess().observe(this, success -> {
            if (success != null && success) {
                Toast.makeText(this, "Lozinka je uspešno promenjena.", Toast.LENGTH_LONG).show();
                finish();
            }
        });

        authViewModel.getIsLoading().observe(this, isLoading -> {
            btnSubmitResetPassword.setEnabled(isLoading == null || !isLoading);
        });

        btnSubmitResetPassword.setOnClickListener(v -> {
            String oldPass = etOldPassword.getText() != null ? etOldPassword.getText().toString().trim() : "";
            String newPass = etNewPassword.getText() != null ? etNewPassword.getText().toString().trim() : "";
            String repeatPass = etRepeatNewPassword.getText() != null ? etRepeatNewPassword.getText().toString().trim() : "";

            authViewModel.updatePassword(oldPass, newPass, repeatPass);
        });
    }
}
