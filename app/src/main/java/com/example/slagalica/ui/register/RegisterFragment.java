package com.example.slagalica.ui.register;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.slagalica.R;
import com.example.slagalica.ui.auth.AuthViewModel;
import com.example.slagalica.ui.region.RegionCoordinates;
import com.google.android.material.textfield.TextInputEditText;

public class RegisterFragment extends Fragment {

    private AuthViewModel authViewModel;
    private AutoCompleteTextView actvRegion;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        TextInputEditText etEmail    = view.findViewById(R.id.etEmailRegister);
        TextInputEditText etUsername = view.findViewById(R.id.etUsername);
        actvRegion                   = view.findViewById(R.id.actvRegion);
        TextInputEditText etPassword = view.findViewById(R.id.etPasswordRegister);
        TextInputEditText etRepeat   = view.findViewById(R.id.etRepeatPassword);
        Button btnSubmit             = view.findViewById(R.id.btnSubmitRegister);

        String[] regions = RegionCoordinates.getAllRegions();
        ArrayAdapter<String> regionAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                regions
        );
        actvRegion.setAdapter(regionAdapter);
        actvRegion.setText(regions[0], false);

        authViewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
        });

        authViewModel.getRegisterSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null && success) {
                Toast.makeText(requireContext(), R.string.register_toast_message, Toast.LENGTH_LONG).show();
                Navigation.findNavController(view).popBackStack();
            }
        });

        authViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            btnSubmit.setEnabled(isLoading == null || !isLoading);
        });

        btnSubmit.setOnClickListener(v -> {
            String email    = etEmail.getText()    != null ? etEmail.getText().toString().trim()    : "";
            String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
            String region   = actvRegion.getText() != null ? actvRegion.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
            String repeat   = etRepeat.getText()   != null ? etRepeat.getText().toString().trim()   : "";

            boolean validRegion = false;
            for (String r : regions) {
                if (r.equals(region)) { validRegion = true; break; }
            }
            if (!validRegion) {
                Toast.makeText(requireContext(), "Izaberi region sa liste.", Toast.LENGTH_SHORT).show();
                return;
            }

            authViewModel.register(email, username, region, password, repeat);
        });

        Button btnGoToLogin = view.findViewById(R.id.btnGoToLogin);
        if (btnGoToLogin != null) {
            btnGoToLogin.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());
        }
    }
}