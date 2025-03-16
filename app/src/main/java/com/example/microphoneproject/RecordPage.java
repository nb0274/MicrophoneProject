package com.example.microphoneproject;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import Objects.Record;
import Objects.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public class RecordPage extends AppCompatActivity {

    private Button btnRecord, btnPauseContinue, btnCancel, btnDone;
    private MediaRecorder mediaRecorder;
    private long totalPausedTime = 0, pauseStartTime = 0;

    private boolean isRecording = false, isPaused = false;
    private String audioFilePath;
    private long startTime, pausedTime = 0;

    private final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private User user = User.getInstance();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_page);
        checkPermissions();

        btnRecord = findViewById(R.id.btnRecord);
        btnPauseContinue = findViewById(R.id.btnPauseContinue);
        btnCancel = findViewById(R.id.btnCancel);
        btnDone = findViewById(R.id.btnDone);

        audioFilePath = getExternalCacheDir().getAbsolutePath() + "/record_" + UUID.randomUUID() + ".mp4";

        btnPauseContinue.setEnabled(false);
        btnCancel.setEnabled(false);
        btnDone.setEnabled(false);
        resetButtons();
    }

    // onClick method for the "Record" button
    public void onRecordClicked(View view) {
        startRecording();
    }

    // onClick method for the "Pause/Continue" button
    public void onPauseContinueClicked(View view) {
        togglePauseContinue();
    }

    // onClick method for the "Cancel" button
    public void onCancelClicked(View view) {
        cancelRecording();
    }

    // onClick method for the "Done" button
    public void onDoneClicked(View view) {
        finishRecording();
    }

    private void startRecording() {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setOutputFile(audioFilePath);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            startTime = System.currentTimeMillis();
            totalPausedTime = 0;
            isRecording = true;

            btnRecord.setEnabled(false);
            btnPauseContinue.setEnabled(true);
            btnCancel.setEnabled(true);
            btnDone.setEnabled(true);
            Toast.makeText(this, "Recording started...", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Recording failed to start.", Toast.LENGTH_SHORT).show();
        }
    }

    private void togglePauseContinue() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            if (isPaused) {
                mediaRecorder.resume();
                btnPauseContinue.setText("Pause");
                totalPausedTime += System.currentTimeMillis() - pauseStartTime; // accumulate paused duration
                isPaused = false;
            } else {
                mediaRecorder.pause();
                btnPauseContinue.setText("Continue");
                pauseStartTime = System.currentTimeMillis(); // mark when paused
                isPaused = true;
            }
        } else {
            Toast.makeText(this, "Pause/Continue not supported on this device.", Toast.LENGTH_SHORT).show();
        }
    }

    private void cancelRecording() {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
        }
        resetButtons();
        File file = new File(audioFilePath);
        if (file.exists()) file.delete();
        Toast.makeText(this, "Recording canceled.", Toast.LENGTH_SHORT).show();
    }

    private void finishRecording() {
        if (mediaRecorder != null) {
            if (isPaused && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                mediaRecorder.resume(); // Ensure it's not paused
            }
            try {
                mediaRecorder.stop();
            } catch (RuntimeException e) {
                e.printStackTrace(); // Handle invalid stop
            }
            mediaRecorder.release();
            mediaRecorder = null;
        }

        long durationMillis = System.currentTimeMillis() - startTime - totalPausedTime;
        double durationSeconds = durationMillis / 1000.0;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Save Recording");
        builder.setMessage("Enter a name for your recording:");

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String rName = input.getText().toString().trim();
            if (!rName.isEmpty()) {
                checkAndSaveRecording(rName, durationSeconds);
            } else {
                Toast.makeText(this, "Recording name cannot be empty.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();

        resetButtons(); // Move this here to reset state after finishing recording
    }

    private void checkAndSaveRecording(String rName, double duration) {
        String uid = user.getUID();

        FBRef.refRecordings.child(uid).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                boolean exists = false;
                for (DataSnapshot snapshot : task.getResult().getChildren()) {
                    String existingName = snapshot.child("rname").getValue(String.class);
                    if (rName.equals(existingName)) {
                        exists = true;
                        break;
                    }
                }
                if (exists) {
                    Toast.makeText(RecordPage.this, "You already have a recording with that name.", Toast.LENGTH_SHORT).show();
                } else {
                    uploadAudioFile(uid, rName, duration);
                }
            } else {
                Toast.makeText(RecordPage.this, "Failed to check existing recordings.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadAudioFile(String uid, String rName, double duration) {
        File file = new File(audioFilePath);
        if (!file.exists()) {
            Toast.makeText(this, "No audio file found!", Toast.LENGTH_SHORT).show();
            return;
        }

        String rid = UUID.randomUUID().toString();
        StorageReference storageRef = FirebaseStorage.getInstance().getReference("audio_files/" + rid + ".3gp");

        storageRef.putFile(Uri.fromFile(file))
                .addOnProgressListener(taskSnapshot -> {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    Toast.makeText(RecordPage.this, "Uploading: " + (int) progress + "%", Toast.LENGTH_SHORT).show();
                })
                .addOnSuccessListener(taskSnapshot -> {
                    // Upload success, now save record object
                    Record record = new Record(duration, rid, uid, rName);
                    FBRef.refRecordings.child(uid).child(rid).setValue(record)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(RecordPage.this, "Recording saved successfully.", Toast.LENGTH_SHORT).show();
                                file.delete(); // Delete local file to save space
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(RecordPage.this, "Failed to save recording metadata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RecordPage.this, "Failed to upload recording: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }



    private void resetButtons() {
        btnRecord.setEnabled(true);
        btnPauseContinue.setEnabled(false);
        btnCancel.setEnabled(false);
        btnDone.setEnabled(false);
        btnPauseContinue.setText("Pause");
        isRecording = false;
        isPaused = false;
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu); // Replace with your menu file name if different
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menuLogIn) {
            startActivity(new Intent(RecordPage.this, MainActivity.class));
            return true;
        } else if (id == R.id.menuSignUp) {
            startActivity(new Intent(RecordPage.this, SignupPage.class));
            return true;
        } else if (id == R.id.menuRecordPage) {
            // Already in Record Page activity; no need for action here
            return true;
        } else if (id == R.id.menuRecordList) {
            startActivity(new Intent(RecordPage.this, RecordsList.class));
            return true;
        } else if (id == R.id.menuAlphaBtnRecord) {
            startActivity(new Intent(RecordPage.this, Alpha_BtnRecord.class));
            return true;
        } else if (id == R.id.menuAlphaChooseFile) {
            startActivity(new Intent(RecordPage.this, Alpha_ChooseFile.class));
            return true;
        } else if (id == R.id.menuStorageImport) {
            startActivity(new Intent(RecordPage.this, Alpha_StorageImport.class));
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
}