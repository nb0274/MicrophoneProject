package com.example.microphoneproject;
//l
import android.annotation.SuppressLint;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.Locale;

public class PlayRecordingActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private Button btnPlayPause, btnRewind10, btnRewind5, btnForward5, btnForward10;
    private TextView txtTimer;
    private boolean isPlaying = false;
    private boolean isPrepared = false; // Ensures media is ready before playing
    private String rid;  // Only storing rid
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_recording);

        btnPlayPause = findViewById(R.id.btnPlayPause);
        txtTimer = findViewById(R.id.txtTimer);

        rid = getIntent().getStringExtra("RECORD_ID"); // Get rid from intent

        if (rid == null || rid.isEmpty()) {
            Toast.makeText(this, "Error: Invalid recording ID.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Fetch audio from Firebase Storage
        StorageReference storageRef = FirebaseStorage.getInstance().getReference("audio_files/" + rid + ".mp4");
        storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
            initializeMediaPlayer(uri);
        }).addOnFailureListener(e -> {
            Toast.makeText(PlayRecordingActivity.this, "Failed to fetch audio: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void initializeMediaPlayer(Uri audioUri) {
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(this, audioUri);
            mediaPlayer.prepareAsync(); // Prepare asynchronously to avoid blocking UI
            mediaPlayer.setOnPreparedListener(mp -> {
                isPrepared = true;
                updateTimerDisplay(); // Show initial total duration
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
                btnPlayPause.setText("Play");
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void togglePlayPause(View view) {
        if (mediaPlayer == null || !isPrepared) return; // Ensure media is ready

        if (isPlaying) {
            mediaPlayer.pause();
            btnPlayPause.setText("Play");
        } else {
            mediaPlayer.start();
            btnPlayPause.setText("Pause");
            startUpdatingTimer();
        }
        isPlaying = !isPlaying;
    }

    private void startUpdatingTimer() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && isPlaying) {
                    updateTimerDisplay();
                    handler.postDelayed(this, 500); // Update every 500ms
                }
            }
        }, 500);
    }

    private void updateTimerDisplay() {
        if (mediaPlayer != null && isPrepared) {
            int currentPos = mediaPlayer.getCurrentPosition() / 1000; // Convert to seconds
            int totalDuration = mediaPlayer.getDuration() / 1000; // Convert to seconds
            txtTimer.setText(formatTime(currentPos) + " / " + formatTime(totalDuration));
        }
    }

    private String formatTime(int seconds) {
        return String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60);
    }

    public void rewind10(View view) {
        rewind(10000);
    }

    public void rewind5(View view) {
        rewind(5000);
    }

    public void forward5(View view) {
        forward(5000);
    }

    public void forward10(View view) {
        forward(10000);
    }


    private void rewind(int milliseconds) {
        if (mediaPlayer != null) {
            int newPosition = Math.max(mediaPlayer.getCurrentPosition() - milliseconds, 0);
            mediaPlayer.seekTo(newPosition);
        }
    }

    private void forward(int milliseconds) {
        if (mediaPlayer != null) {
            int newPosition = Math.min(mediaPlayer.getCurrentPosition() + milliseconds, mediaPlayer.getDuration());
            mediaPlayer.seekTo(newPosition);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }
}
