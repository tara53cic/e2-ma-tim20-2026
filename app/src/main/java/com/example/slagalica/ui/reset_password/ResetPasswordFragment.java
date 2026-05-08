package com.example.slagalica.ui.reset_password;

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

public class ResetPasswordFragment extends Fragment {

    private AuthViewModel authViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reset_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        TextInputEditText etOldPassword = view.findViewById(R.id.etOldPassword);
        TextInputEditText etNewPassword = view.findViewById(R.id.etNewPassword);
        TextInputEditText etRepeatNewPassword = view.findViewById(R.id.etRepeatNewPassword);
        Button btnSubmitResetPassword = view.findViewById(R.id.btnSubmitResetPassword);

        authViewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
            }
        });

        authViewModel.getPasswordResetSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null && success) {
                Toast.makeText(requireContext(), "Lozinka je uspešno promenjena.", Toast.LENGTH_LONG).show();
                Navigation.findNavController(view).popBackStack();
            }
        });

        authViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
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
