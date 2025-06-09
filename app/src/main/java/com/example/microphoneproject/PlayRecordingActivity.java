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

    /**
     * Initializes and prepares a {@link MediaPlayer} instance to play audio from the given Uri.
     * <p>
     * Sets the data source, prepares the media player asynchronously, and sets up listeners
     * for when preparation is complete (to update UI and enable playback) and when playback
     * completes (to reset playback state). Handles potential IOExceptions during setup.
     *
     * @param audioUri The URI of the audio content to be played.
     */
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

    /**
     * Toggles the playback state of the {@link MediaPlayer} between play and pause.
     * <p>
     * If the media player is initialized and prepared, this method will either pause
     * playback if it's currently playing, or start/resume playback if it's paused.
     * It also updates the text of a play/pause button ({@code btnPlayPause}) and
     * starts a timer update when playback begins.
     *
     * @param view The View that triggered this action (e.g., a play/pause button).
     */
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

    /**
     * Starts a recurring task using a {@link Handler} to update the timer display every 500ms.
     * <p>
     * This task will continue to run as long as the {@code mediaPlayer} is not null and
     * {@code isPlaying} is true. Each execution calls {@code updateTimerDisplay()}
     * and then reschedules itself.
     */
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

    /**
     * Updates the {@code txtTimer} TextView to display the current playback position and total duration.
     * <p>
     * If the {@link MediaPlayer} is prepared, this method retrieves the current position and
     * total duration (in seconds), formats them using {@code formatTime()}, and sets the
     * combined string on the {@code txtTimer}.
     */
    private void updateTimerDisplay() {
        if (mediaPlayer != null && isPrepared) {
            int currentPos = mediaPlayer.getCurrentPosition() / 1000; // Convert to seconds
            int totalDuration = mediaPlayer.getDuration() / 1000; // Convert to seconds
            txtTimer.setText(formatTime(currentPos) + " / " + formatTime(totalDuration));
        }
    }

    /**
     * Formats a given duration in seconds into a "MM:SS" string.
     *
     * @param seconds The total number of seconds to format.
     * @return A string representation of the time in MM:SS format (e.g., "01:30").
     */
    private String formatTime(int seconds) {
        return String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60);
    }

    /**
     * Rewinds the media playback by 10 seconds.
     * <p>
     * This method is intended to be called from a UI element (e.g., a button)
     * and delegates to the private {@code rewind(int)} method with a value of 10000 milliseconds.
     *
     * @param view The View that triggered this action (e.g., a rewind button).
     */
    public void rewind10(View view) {
        rewind(10000);
    }

    /**
     * Rewinds the media playback by 5 seconds.
     * <p>
     * This method is intended to be called from a UI element (e.g., a button)
     * and delegates to the private {@code rewind(int)} method with a value of 5000 milliseconds.
     *
     * @param view The View that triggered this action (e.g., a rewind button).
     */
    public void rewind5(View view) {
        rewind(5000);
    }

    /**
     * Forwards the media playback by 5 seconds.
     * <p>
     * This method is intended to be called from a UI element (e.g., a button)
     * and delegates to the private {@code forward(int)} method with a value of 5000 milliseconds.
     *
     * @param view The View that triggered this action (e.g., a forward button).
     */
    public void forward5(View view) {
        forward(5000);
    }

    /**
     * Forwards the media playback by 10 seconds.
     * <p>
     * This method is intended to be called from a UI element (e.g., a button)
     * and delegates to the private {@code forward(int)} method with a value of 10000 milliseconds.
     *
     * @param view The View that triggered this action (e.g., a forward button).
     */
    public void forward10(View view) {
        forward(10000);
    }

    /**
     * Rewinds the media playback by the specified number of milliseconds.
     * <p>
     * If the {@link MediaPlayer} instance is not null, it calculates the new playback position
     * by subtracting the given milliseconds from the current position, ensuring the new position
     * does not go below zero. It then seeks the MediaPlayer to this new position.
     *
     * @param milliseconds The number of milliseconds to rewind the playback.
     */
    private void rewind(int milliseconds) {
        if (mediaPlayer != null) {
            int newPosition = Math.max(mediaPlayer.getCurrentPosition() - milliseconds, 0);
            mediaPlayer.seekTo(newPosition);
        }
    }

    /**
     * Forwards the media playback by the specified number of milliseconds.
     * <p>
     * If the {@link MediaPlayer} instance is not null, it calculates the new playback position
     * by adding the given milliseconds to the current position, ensuring the new position
     * does not exceed the media's total duration. It then seeks the MediaPlayer to this new position.
     *
     * @param milliseconds The number of milliseconds to forward the playback.
     */
    private void forward(int milliseconds) {
        if (mediaPlayer != null) {
            int newPosition = Math.min(mediaPlayer.getCurrentPosition() + milliseconds, mediaPlayer.getDuration());
            mediaPlayer.seekTo(newPosition);
        }
    }

    /**
     * Called when the activity is being destroyed.
     * <p>
     * Releases the {@link MediaPlayer} instance if it exists to free up system resources.
     * This is crucial to prevent resource leaks.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }
}
