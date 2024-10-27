package com.example.echolocation;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
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

import androidx.annotation.NonNull;
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
    private TextView soundLevelTextView, timerTextView, micStatusTextView;
    private Button toggleButton;
    private ImageView dolphinImageView;
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
        if (timerCount % 5 == 0) {
            if (isRecording) {
                stopRecording();
                micStatusTextView.setText("Mic: OFF");
            } else {
                startRecording();
                micStatusTextView.setText("Mic: ON");
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
                adjustBackgroundColor((float) Math.max(0, Math.round(dB)));
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

    private void adjustBackgroundColor(float dB) {
        int alpha = (int) Math.max(0, Math.min(255, 255 * (1 - (dB / 100.0))));
        findViewById(R.id.main).setBackgroundColor(Color.argb(alpha, 0, 0, 0));
    }

    private void startCoralAnimation() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                moveCorals();
                handler.postDelayed(this, 5000);
            }
        }, 5000);
    }

    private void moveCorals() {
        for (ImageView coral : coralImages) {
            int x = random.nextInt(screenWidth - coral.getWidth());
            int y = random.nextInt(screenHeight - coral.getHeight());

            coral.setX(x);
            coral.setY(y);
            coral.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float tiltX = event.values[0];
            float tiltY = event.values[1];

            // Update position based on tilt without any boundaries
            dolphinX -= tiltX * MOVEMENT_FACTOR;
            dolphinY += tiltY * MOVEMENT_FACTOR;

            if (dolphinX < 0) {
                dolphinX = screenWidth;
            } else if (dolphinX > screenWidth) {
                dolphinX = 0;
            }

            if (dolphinY < 0) {
                dolphinY = screenHeight;
            } else if (dolphinY > screenHeight) {
                dolphinY = 0;
            }

// Set the new position for the dolphin
            dolphinImageView.setX(dolphinX);
            dolphinImageView.setY(dolphinY);
        }
    }


    private void updateDolphinPosition(float tiltX, float tiltY) {
        dolphinX -= tiltX * MOVEMENT_FACTOR;
        dolphinY += tiltY * MOVEMENT_FACTOR;

        dolphinX = Math.max(0, Math.min(dolphinX, screenWidth - dolphinImageView.getWidth()));
        dolphinY = Math.max(0, Math.min(dolphinY, screenHeight - dolphinImageView.getHeight()));

        dolphinImageView.setX(dolphinX);
        dolphinImageView.setY(dolphinY);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        stopRecording(); // Ensure to stop recording when the activity pauses
    }
}
