package com.example.microphoneproject;
//l
import static com.example.microphoneproject.FBRef.refAuth;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;

import Objects.User;

public class Alpha_BtnRecord extends AppCompatActivity {

    private Button recordButton, uploadButton;
    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private boolean isRecording = false;
    private String audioFilePath;
    private User user;

    private static final int REQUEST_RECORD_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alpha_btn_record);
        checkPermissions();

        recordButton = findViewById(R.id.recordButton);
        uploadButton = findViewById(R.id.uploadButton);

        // Set up file path for audio recording
        audioFilePath = getExternalCacheDir().getAbsolutePath() + "/audio_record.3gp";

        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRecording) {
                    stopRecording();
                } else {
                    startRecording();
                }
            }
            ;
        });

        user.setUID(refAuth.getCurrentUser().getUid());
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadAudioToFirebase();
            }
        });
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
            isRecording = true;
            recordButton.setText("Stop Recording");
            Toast.makeText(this, "Recording started...", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e("AudioRecorder", "Recording failed", e);
        }
    }

    private void stopRecording() {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;
            recordButton.setText("Record");
            Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show();

            // Play the recorded audio
            playRecording();
        }
    }

    private void playRecording() {
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(audioFilePath);
            mediaPlayer.prepare();
            mediaPlayer.start();
            Toast.makeText(this, "Playing recording...", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e("AudioRecorder", "Playback failed", e);
        }
    }

    private void uploadAudioToFirebase() {
        File audioFile = new File(audioFilePath);
        if (!audioFile.exists()) {
            Toast.makeText(this, "No audio file to upload", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference().child("audio_files/" + audioFile.getName());

        storageRef.putFile(Uri.fromFile(audioFile))
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Toast.makeText(Alpha_BtnRecord.this, "Audio uploaded successfully", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(Alpha_BtnRecord.this, "Failed to upload audio", Toast.LENGTH_SHORT).show();
                        Log.e("AudioRecorder", "Upload failed", e);
                    }
                });
    }


    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
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
            startActivity(new Intent(Alpha_BtnRecord.this, MainActivity.class));
            return true;
        } else if (id == R.id.menuSignUp) {
            startActivity(new Intent(Alpha_BtnRecord.this, SignupPage.class));
            return true;
        } else if (id == R.id.menuRecordPage) {
            startActivity(new Intent(Alpha_BtnRecord.this, RecordPage.class));
            return true;
        } else if (id == R.id.menuRecordList) {
            startActivity(new Intent(Alpha_BtnRecord.this, RecordsList.class));
            return true;
        } else if (id == R.id.menuAlphaBtnRecord) {
            // Already in Button Record activity; no need for action here
            return true;
        } else if (id == R.id.menuAlphaChooseFile) {
            startActivity(new Intent(Alpha_BtnRecord.this, Alpha_ChooseFile.class));
            return true;
        } else if (id == R.id.menuStorageImport) {
            startActivity(new Intent(Alpha_BtnRecord.this, Alpha_StorageImport.class));
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
}