package com.example.echolocation;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.Random;
import android.graphics.Point;

public class Echosound extends AppCompatActivity implements SensorEventListener {

    private static final int REQUEST_AUDIO_PERMISSION_CODE = 1;
    private static final float MOVEMENT_FACTOR = 5f; // Sensitivity for dolphin movement
    private static final int SAMPLE_RATE = 44100;

    // UI Elements
    private TextView soundLevelTextView, timerTextView, micStatusTextView, scoreTextView;
    private Button toggleButton;
    private ImageView dolphinImageView, orcaImageView;
    private ImageView[] coralImages;

    // Audio and Sensor Management
    private AudioRecord audioRecord;
    private Handler handler;
    private SensorManager sensorManager;
    private Sensor accelerometer;

    // Randomness for coral movement
    private Random random;

    // Dolphin position
    private float dolphinX, dolphinY;

    // Screen dimensions
    private int screenWidth, screenHeight;
    private int bufferSize;
    private int timerCount;
    private boolean isRecording = false;
    private boolean isGameOver = false;
    private int score = 0;
    private boolean isImmune = true; // Immunity period
    private Rect dolphinRect = new Rect();
    private Rect coralRect = new Rect();
    private Rect orcaRect = new Rect();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.echosound);

        // Screen setup
        setupScreenDimensions();

        // UI elements
        initializeUIElements();

        // Initialize random and handler
        random = new Random();
        handler = new Handler();

        // Request microphone permission
        requestMicrophonePermission();

        // Set up sensors and listeners
        setupSensor();
        setupListeners();

        // Start timer and coral animation
        startTimer();
        startCoralAnimation();

        // Initialize dolphin position
        dolphinX = dolphinImageView.getX();
        dolphinY = dolphinImageView.getY();

        startOrcaMovement();
        // Enable immunity for the first 2 seconds
        enableImmunityPeriod();
    }

    private void startOrcaMovement(){
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(!isGameOver){
                    float baseSpeed = 3f; // Base speed for orca
                    float speedMultiplier = 1 + (timerCount / 20f);
                    float deltaX = dolphinX - orcaImageView.getX();
                    float deltaY = dolphinY - orcaImageView.getY();

                    float distance = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                    float moveX = (deltaX / distance) * 3f; // Adjust speed here
                    float moveY = (deltaY / distance) * 3f;

                    // Update orca's position
                    orcaImageView.setX(orcaImageView.getX() + moveX);
                    orcaImageView.setY(orcaImageView.getY() + moveY);

                    // Check for collision between orca and dolphin
                    checkOrcaCollision();

                    // Repeat movement every 50 ms
                    handler.postDelayed(this, 50);
                }
            }
        }, 50);
    }

    private void checkOrcaCollision() {
        if (isGameOver) return;

        // Define the boundaries of the orca and dolphin for collision
        orcaRect.left = (int) orcaImageView.getX();
        orcaRect.top = (int) orcaImageView.getY();
        orcaRect.right = (int) orcaImageView.getX() + orcaImageView.getWidth() - 20;
        orcaRect.bottom = (int) orcaImageView.getY() + orcaImageView.getHeight() - 20;

        dolphinRect.left = (int) dolphinX;
        dolphinRect.top = (int) dolphinY;
        dolphinRect.right = (int) dolphinX + dolphinImageView.getWidth()-10;
        dolphinRect.bottom = (int) dolphinY + dolphinImageView.getHeight()-10;

        // Trigger game over if orca intersects with dolphin
        if (Rect.intersects(dolphinRect, orcaRect)) {
            showGameOverDialog();
        }
    }

    private void enableImmunityPeriod() {
        isImmune = true;
        handler.postDelayed(() -> isImmune = false, 2000); // Disable immunity after 2 seconds
    }

    private void updateScore(int newScore) {
        score = newScore;
        runOnUiThread(() -> scoreTextView.setText("Score: " + score));
    }

    // Screen setup
    private void setupScreenDimensions() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;
    }

    // Initialize UI elements
    private void initializeUIElements() {
        soundLevelTextView = findViewById(R.id.soundLevelTextView);
        timerTextView = findViewById(R.id.timerTextView);
        micStatusTextView = findViewById(R.id.micStatusTextView);
        toggleButton = findViewById(R.id.toggleButton);
        dolphinImageView = findViewById(R.id.imageView3);
        orcaImageView = findViewById(R.id.orcaImageView);
        scoreTextView = findViewById(R.id.scoreTextView);

        coralImages = new ImageView[]{
                findViewById(R.id.imageView5),
                findViewById(R.id.imageView6),
                findViewById(R.id.imageView7)
        };
    }

    // Request microphone permission
    private void requestMicrophonePermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO_PERMISSION_CODE);
    }

    // Setting up sensors
    private void setupSensor() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    // Setting up button and mic toggling listeners
    private void setupListeners() {
        toggleButton.setOnClickListener(v -> {
            if (isRecording) {
                stopRecording();
                toggleButton.setText("Start Listening");
            } else {
                if (checkPermission()) {
                    startRecording();
                    toggleButton.setText("Stop Listening");
                } else {
                    requestMicrophonePermission();
                }
            }
        });
    }

    private void startTimer() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                timerCount++;
                timerTextView.setText("Time: " + timerCount + "s");
                toggleMic();
                handler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    private void toggleMic() {
        if (timerCount % 10 == 0) {
            if (isRecording) {
                stopRecording();
                micStatusTextView.setText("Mic: OFF");
                setCoralVisibility(false); // Make corals not visible
            } else {
                startRecording();
                micStatusTextView.setText("Mic: ON");
                setCoralVisibility(true); // Make corals visible
            }
        }
    }

    private void startRecording() {
        bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (checkPermission()) {
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
            audioRecord.startRecording();
            isRecording = true;
            updateSoundLevel();
        }
    }

    private void updateSoundLevel() {
        if (isRecording && audioRecord != null) {
            short[] buffer = new short[bufferSize];
            int read = audioRecord.read(buffer, 0, buffer.length);
            if (read > 0) {
                double amplitude = calculateAmplitude(buffer, read);
                double dB = 10 * Math.log10(amplitude);

                soundLevelTextView.setText("Sound Level: " + Math.round(dB) + " dB");
                adjustCoralOpacity((float) Math.max(0, Math.round(dB)));
            }
            handler.postDelayed(this::updateSoundLevel, 1000);
        }
    }

    private double calculateAmplitude(short[] buffer, int read) {
        double sum = 0;
        for (short value : buffer) {
            sum += value * value;
        }
        return sum / read;
    }

    private void stopRecording() {
        if (isRecording && audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            isRecording = false;
            audioRecord = null;

            // Make corals not visible or set low opacity
            setCoralVisibility(false); // Set to invisible when recording stops
        }
    }

    private boolean checkPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_AUDIO_PERMISSION_CODE) {
            if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                finish();
            }
        }
    }

    private void adjustCoralOpacity(float dB) {
        int alpha;
        if (dB < 55) {
            alpha = 0;
        } else if (dB < 65) {
            alpha = 100;
        } else if (dB < 75) {
            alpha = 150;
        } else if (dB < 85) {
            alpha = 200;
        } else {
            alpha = 250;
        }

        for (ImageView coral : coralImages) {
            coral.setImageAlpha(alpha);
        }
    }

    private void setCoralVisibility(boolean isVisible) {
        int alpha = isVisible ? 255 : 0;
        for (ImageView coral : coralImages) {
            coral.setImageAlpha(alpha);
        }
    }

    private void startCoralAnimation() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                moveCorals();
                handler.postDelayed(this, 5000);
            }
        }, 8000);
    }

    private void checkCollision() {
        if (isGameOver || isImmune) return;

        dolphinRect.left = (int) dolphinX;
        dolphinRect.top = (int) dolphinY;
        dolphinRect.right = (int) dolphinX + dolphinImageView.getWidth() - 10;
        dolphinRect.bottom = (int) dolphinY + dolphinImageView.getHeight() - 10;

        for (ImageView coral : coralImages) {
            if (coral.getImageAlpha() > 0) {
                coralRect.left = (int) coral.getX();
                coralRect.top = (int) coral.getY();
                coralRect.right = (int) coral.getX() + coral.getWidth() - 20;
                coralRect.bottom = (int) coral.getY() + coral.getHeight() - 20;

                if (Rect.intersects(dolphinRect, coralRect)) {
                    // Game over scenario
                    isGameOver = true;
                    showGameOverDialog();
                    break;
                }
            }
        }
    }

    private void showGameOverDialog() {
        runOnUiThread(() -> {
            new AlertDialog.Builder(Echosound.this)
                    .setTitle("Game Over")
                    .setMessage("Your score: " + score + "\nTime survived: " + timerCount + " seconds")
                    .setPositiveButton("Play Again", (dialog, which) -> resetGame())
                    .setNegativeButton("Exit", (dialog, which) -> finish())
                    .setCancelable(false)
                    .show();
        });
    }

    private void resetGame() {
        score = 0;
        timerCount = 0;
        isGameOver = false;
        isImmune = true;
        enableImmunityPeriod();
        updateScore(score);

        dolphinX = screenWidth / 2f - dolphinImageView.getWidth() / 2f;
        dolphinY = screenHeight / 2f - dolphinImageView.getHeight() / 2f;
        dolphinImageView.setX(dolphinX);
        dolphinImageView.setY(dolphinY);

        orcaImageView.setX(0); // Reset orca's position if desired
        orcaImageView.setY(0);

        // Restart orca movement
        startOrcaMovement();
    }

    private void moveCorals() {
        for (ImageView coral : coralImages) {
            float randomX = random.nextFloat() * (screenWidth - coral.getWidth());
            float randomY = random.nextFloat() * (screenHeight - coral.getHeight());
            coral.setX(randomX);
            coral.setY(randomY);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && !isGameOver) {
            float deltaX = -event.values[0] * MOVEMENT_FACTOR;
            float deltaY = event.values[1] * MOVEMENT_FACTOR;

            dolphinX = Math.max(0, Math.min(dolphinX + deltaX, screenWidth - dolphinImageView.getWidth()));
            dolphinY = Math.max(0, Math.min(dolphinY + deltaY, screenHeight - dolphinImageView.getHeight()));

            dolphinImageView.setX(dolphinX);
            dolphinImageView.setY(dolphinY);

            checkCollision();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (accelerometer != null) {
            sensorManager.unregisterListener(this);
        }
        stopRecording();
    }
}

