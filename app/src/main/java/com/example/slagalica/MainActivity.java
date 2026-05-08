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

public class MainActivity extends AppCompatActivity {

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
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_main);

        if (navHostFragment != null && navView != null) {
            NavController navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(navView, navController);

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
    }
}
