package com.example.slagalica.ui.home;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.slagalica.MainActivity;
import com.example.slagalica.R;
import com.example.slagalica.ui.auth.AuthViewModel;
import com.example.slagalica.ui.profile.ProfileViewModel;
import com.example.slagalica.ui.reset_password.ResetPasswordActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.graphics.Bitmap;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.material.imageview.ShapeableImageView;

public class ProfileFragment extends Fragment {

    private static final int[] AVATAR_COLORS = {
            0xFF508EFA, 0xFF2EC27E, 0xFFE53935,
            0xFFF4A261, 0xFF9C27B0, 0xFF00BCD4
    };

    private static final int[] AVATAR_ICONS = {
            R.drawable.ic_avatar_1,
            R.drawable.ic_avatar_2,
            R.drawable.ic_avatar_3,
            R.drawable.ic_avatar_4,
            R.drawable.ic_avatar_5,
            R.drawable.ic_avatar_6
    };

    private static final String[] LEAGUE_NAMES = {
            "Početna", "Bronzana", "Srebrna", "Zlatna", "Platinasta", "Dijamantska"
    };

    private static final int[] LEAGUE_ICONS = {
            R.drawable.ic_league_0,
            R.drawable.ic_league_1,
            R.drawable.ic_league_2,
            R.drawable.ic_league_3,
            R.drawable.ic_league_4,
            R.drawable.ic_league_5
    };

    private AuthViewModel authViewModel;
    private ProfileViewModel profileViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        ShapeableImageView ivAvatar = view.findViewById(R.id.ivAvatar);
        View ivEditAvatar = view.findViewById(R.id.ivEditAvatar);
        TextView tvChangePassword = view.findViewById(R.id.tvChangePassword);
        Button btnLogout = view.findViewById(R.id.btnLogout);
        View btnViewStats = view.findViewById(R.id.btnViewStats);
        com.google.android.material.button.MaterialButton btnToggleQr = view.findViewById(R.id.btnToggleQr);
        android.widget.ImageView ivQrCode = view.findViewById(R.id.ivQrCode);
        TextView tvUsername = view.findViewById(R.id.tvUsername);
        TextView tvEmail = view.findViewById(R.id.tvEmail);
        TextView tvProfileTokens = view.findViewById(R.id.tvProfileTokens);
        TextView tvProfileStars = view.findViewById(R.id.tvProfileStars);
        TextView tvLeague = view.findViewById(R.id.tvLeague);
        TextView tvRegion = view.findViewById(R.id.tvRegion);


        profileViewModel.getUsername().observe(getViewLifecycleOwner(), name -> {
            if (name != null) tvUsername.setText(name);
        });

        profileViewModel.getEmail().observe(getViewLifecycleOwner(), mail -> {
            if (mail != null) tvEmail.setText(mail);
        });

        profileViewModel.getRegion().observe(getViewLifecycleOwner(), reg -> {
            if (reg != null) tvRegion.setText(reg);
        });

        profileViewModel.getTokens().observe(getViewLifecycleOwner(), tok -> {
            if (tok != null) tvProfileTokens.setText(String.valueOf(Math.max(0,tok)));
        });

        profileViewModel.getStars().observe(getViewLifecycleOwner(), s -> {
            if (s != null) tvProfileStars.setText(String.valueOf(s));
        });

        ImageView ivLeagueIcon = view.findViewById(R.id.ivLeagueIcon);
        profileViewModel.getLeague().observe(getViewLifecycleOwner(), leagueIndex -> {
            if (leagueIndex != null) {
                int index = (leagueIndex >= 0 && leagueIndex < LEAGUE_NAMES.length)
                        ? leagueIndex : 0;
                tvLeague.setText(LEAGUE_NAMES[index]);
                ivLeagueIcon.setImageResource(LEAGUE_ICONS[index]);
            }
        });

        profileViewModel.getAvatarColorIndex().observe(getViewLifecycleOwner(), colorIndex -> {
            if (colorIndex != null && colorIndex >= 0 && colorIndex < AVATAR_COLORS.length) {
                ivAvatar.setImageResource(AVATAR_ICONS[colorIndex]);
                ivAvatar.setBackgroundTintList(
                        ColorStateList.valueOf(AVATAR_COLORS[colorIndex]));
            }
        });

        profileViewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        });

        //Dugme za statistiku
        btnViewStats.setOnClickListener(v -> {
            if (getActivity() != null) {
                BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_nav);
                if (bottomNav != null) {
                    bottomNav.setSelectedItemId(R.id.nav_statistics);
                }
            }
        });

        // QR
        final boolean[] qrVisible = {false};
        if (btnToggleQr != null && ivQrCode != null) {
            btnToggleQr.setOnClickListener(v -> {
                qrVisible[0] = !qrVisible[0];
                if (qrVisible[0]) {
                    String uid = FirebaseAuth.getInstance().getUid();
                    if (uid != null) {
                        try {
                            MultiFormatWriter writer = new MultiFormatWriter();
                            BitMatrix matrix = writer.encode(uid, BarcodeFormat.QR_CODE, 400, 400);
                            BarcodeEncoder encoder = new BarcodeEncoder();
                            Bitmap bitmap = encoder.createBitmap(matrix);
                            ivQrCode.setImageBitmap(bitmap);
                        } catch (WriterException e) {
                            Toast.makeText(getContext(), "Greška pri generisanju QR koda.", Toast.LENGTH_SHORT).show();
                        }
                    }
                    ivQrCode.setVisibility(android.view.View.VISIBLE);
                    btnToggleQr.setText("Sakrij QR kod");
                } else {
                    ivQrCode.setVisibility(android.view.View.GONE);
                    btnToggleQr.setText("Prikaži QR kod");
                }
            });
        }

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

        ivEditAvatar.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.nav_host_fragment, new AvatarPickerFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }
}
