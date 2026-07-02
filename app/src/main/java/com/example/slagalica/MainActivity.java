package com.example.slagalica;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.slagalica.data.NotificationRepository;
import com.google.android.material.badge.BadgeDrawable;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private NotificationRepository notificationRepository;
    private IncomingRequestManager incomingRequestManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        BottomNavigationView navView = findViewById(R.id.bottom_nav);

        notificationRepository = new NotificationRepository();
        observeUnreadNotifications(navView);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_main);

        if (navHostFragment != null && navView != null) {
            NavController navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(navView, navController);

            String navigateTo = getIntent().getStringExtra("NAVIGATE_TO");
            if ("MATCH".equals(navigateTo)) {
                String matchId = getIntent().getStringExtra("MATCH_ID");
                Bundle args = new Bundle();
                args.putString("MATCH_ID", matchId);
                navController.navigate(R.id.matchFragment, args);
            }

            navView.setOnItemSelectedListener(item -> {
                if (navController.getCurrentDestination() != null &&
                        item.getItemId() != navController.getCurrentDestination().getId()) {
                    navController.navigate(item.getItemId(), null, new androidx.navigation.NavOptions.Builder()
                            .setPopUpTo(navController.getGraph().getStartDestinationId(), false)
                            .setLaunchSingleTop(true)
                            .setRestoreState(false)
                            .build());
                }
                return true;
            });
        }

        setOnlineStatus(true);

        incomingRequestManager = new IncomingRequestManager(this);
        incomingRequestManager.start();

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            new com.example.slagalica.domain.service.DailyTokenService()
                    .checkAndGiveDailyTokens(uid);

            new com.example.slagalica.data.UserRepository().getUser(uid)
                    .addOnSuccessListener(doc -> {
                        if (!doc.exists()) return;
                        String lastReset = doc.getString("lastResetMonth");
                        new com.example.slagalica.domain.service.MonthlyPenaltyService()
                                .applyPenaltyIfNeeded(uid, lastReset);
                    });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setOnlineStatus(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        setOnlineStatus(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        setOnlineStatus(false);
        if (incomingRequestManager != null) incomingRequestManager.stop();
    }

    private void setOnlineStatus(boolean online) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("online", online);
        if (!online) {
            data.put("inGame", false);
        }
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .set(data, com.google.firebase.firestore.SetOptions.merge());
    }

    private void observeUnreadNotifications(BottomNavigationView navView) {
        notificationRepository.observeUnreadCount(count -> {
            if (count > 0) {
                BadgeDrawable badge = navView.getOrCreateBadge(R.id.nav_notifications);
                badge.setVisible(true);
                badge.clearNumber();
                badge.setBackgroundColor(getColor(R.color.error));
            } else {
                navView.removeBadge(R.id.nav_notifications);
            }
        });
    }
}