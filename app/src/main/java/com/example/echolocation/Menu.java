package com.example.echolocation;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import android.widget.Button;
public class Menu extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu);

        // Find the button by its ID
        Button startGameButton = findViewById(R.id.button);

        // Set onClickListener for the button
        startGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create an intent to navigate to MainActivity
                Intent intent = new Intent(Menu.this, HowTo.class);

                // Start the activity
                startActivity(intent);
            }
        });
    }
}
