package com.example.slagalica.ui.game;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.example.slagalica.R;

public class GameActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.game_fragment_container, new NumberGameFragment())
                    .commit();
        }
    }
}
