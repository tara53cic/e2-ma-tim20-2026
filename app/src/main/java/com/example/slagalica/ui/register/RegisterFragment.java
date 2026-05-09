package com.example.slagalica.ui.register;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.slagalica.R;
import com.example.slagalica.ui.auth.AuthViewModel;
import com.google.android.material.textfield.TextInputEditText;

public class RegisterFragment extends Fragment {

    private AuthViewModel authViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        TextInputEditText etEmail = view.findViewById(R.id.etEmailRegister);
        TextInputEditText etUsername = view.findViewById(R.id.etUsername);
        TextInputEditText etRegion = view.findViewById(R.id.etRegion);
        TextInputEditText etPassword = view.findViewById(R.id.etPasswordRegister);
        TextInputEditText etRepeatPassword = view.findViewById(R.id.etRepeatPassword);

        Button btnSubmitRegister = view.findViewById(R.id.btnSubmitRegister);

        authViewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
            }
        });

        authViewModel.getRegisterSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null && success) {
                Toast.makeText(requireContext(), R.string.register_toast_message, Toast.LENGTH_LONG).show();
                Navigation.findNavController(view).popBackStack();
            }
        });

        authViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
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

        Button btnGoToLogin = view.findViewById(R.id.btnGoToLogin);
        if (btnGoToLogin != null) {
            btnGoToLogin.setOnClickListener(v -> {
                Navigation.findNavController(v).popBackStack();
            });
        }
    }
}
