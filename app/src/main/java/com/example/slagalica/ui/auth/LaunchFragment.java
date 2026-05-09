package com.example.slagalica.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.slagalica.MainActivity;
import com.example.slagalica.R;

public class LaunchFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_launch, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btnLogin).setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_launchFragment_to_loginFragment);
        });

        view.findViewById(R.id.btnRegister).setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_launchFragment_to_registerFragment);
        });

        view.findViewById(R.id.btnGuest).setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), MainActivity.class);
            intent.putExtra("NAVIGATE_TO", "MATCH");
            startActivity(intent);
            requireActivity().finish();
        });
    }
}
