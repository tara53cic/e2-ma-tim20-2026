package com.example.slagalica.ui.login;

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

import com.example.slagalica.MainActivity;
import com.example.slagalica.R;
import com.example.slagalica.ui.auth.AuthViewModel;
import com.google.android.material.textfield.TextInputEditText;
import android.content.Intent;

public class LoginFragment extends Fragment {

    private AuthViewModel authViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        TextInputEditText etEmail = view.findViewById(R.id.etEmail);
        TextInputEditText etPassword = view.findViewById(R.id.etPassword);
        Button btnSubmitLogin = view.findViewById(R.id.btnSubmitLogin);
        Button btnGoToRegister = view.findViewById(R.id.btnGoToRegister);

        authViewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
            }
        });

        authViewModel.getLoginSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null && success) {
                Intent intent = new Intent(requireContext(), MainActivity.class);
                startActivity(intent);
                requireActivity().finish();
            }
        });

        authViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            btnSubmitLogin.setEnabled(isLoading == null || !isLoading);
        });

        btnSubmitLogin.setOnClickListener(v -> {
            String emailOrUsername = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

            authViewModel.login(emailOrUsername, password);
        });

        if (btnGoToRegister != null) {
            btnGoToRegister.setOnClickListener(v -> {
                Navigation.findNavController(v).navigate(R.id.action_loginFragment_to_registerFragment);
            });
        }
    }
}
