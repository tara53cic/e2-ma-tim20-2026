package com.example.slagalica.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.slagalica.MainActivity;
import com.example.slagalica.R;
import com.example.slagalica.ui.auth.AuthViewModel;
import com.example.slagalica.ui.reset_password.ResetPasswordActivity;

public class ProfileFragment extends Fragment {

    private AuthViewModel authViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        TextView tvChangePassword = view.findViewById(R.id.tvChangePassword);
        Button btnLogout = view.findViewById(R.id.btnLogout);

        authViewModel.getLogoutSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null && success) {
                Toast.makeText(getContext(), "Uspešno ste se odjavili.", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getActivity(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });

        tvChangePassword.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), ResetPasswordActivity.class));
        });

        btnLogout.setOnClickListener(v -> {
            authViewModel.logout();
        });
    }
}
