package com.example.echolocation;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class HowTo extends AppCompatActivity {

    private Button closeButton;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.howto);

        closeButton = findViewById(R.id.xbutton);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start countdown before launching the main activity
                new CountDownTimer(3000, 1000) {
                    int secondsRemaining = 3;

                    @Override
                    public void onTick(long millisUntilFinished) {
                        closeButton.setText(String.valueOf(secondsRemaining));
                        secondsRemaining--;
                    }

                    @Override
                    public void onFinish() {
                        // Start the main game activity
                        Intent intent = new Intent(HowTo.this, Echosound.class);
                        startActivity(intent);
                        finish(); // Close this activity
                    }
                }.start();
            }
        });
    }
}
