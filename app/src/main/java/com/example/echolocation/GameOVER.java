package com.example.echolocation;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView; // Import TextView if you want to display the timerCount

import androidx.appcompat.app.AppCompatActivity;

public class GameOVER extends AppCompatActivity {

    private int timerCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gameover); // Ensure this layout file exists

        // Retrieve the timerCount from the Intent
        Intent intent = getIntent();
        timerCount = intent.getIntExtra("TIMER_COUNT", 0);

        // Example: Display timerCount in a TextView
        TextView timerTextView = findViewById(R.id.textView2); // Ensure this TextView exists in your layout
        timerTextView.setText("Time: " + timerCount + " seconds");

        TextView scorer = findViewById(R.id.scoreText);
        scorer.setText("Score: " + (timerCount*5));
    }


}
