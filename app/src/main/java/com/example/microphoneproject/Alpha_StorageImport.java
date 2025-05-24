package com.example.microphoneproject;
//l
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;

public class Alpha_StorageImport extends AppCompatActivity {

    private EditText fileNameEditText;
    private Button playButton;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alpha_storage_import);

        fileNameEditText = findViewById(R.id.fileNameEditText);
        playButton = findViewById(R.id.playButton);

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String fileName = fileNameEditText.getText().toString().trim();
                if (fileName.isEmpty()) {
                    Toast.makeText(Alpha_StorageImport.this, "Please enter a file name", Toast.LENGTH_SHORT).show();
                    return;
                }
                downloadAndPlayFile(fileName);
            }
        });
    }

    private void downloadAndPlayFile(String fileName) {
        // Reference to Firebase Storage
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference().child("audio_files/" + fileName);

        // Local file path to save the downloaded audio
        File localFile = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), fileName);

        // Start the file download
        storageRef.getFile(localFile)
                .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        // File successfully downloaded, use the local file path
                        Toast.makeText(Alpha_StorageImport.this, "File downloaded: " + localFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                        playAudio(localFile.getAbsolutePath());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Handle failure
                        Toast.makeText(Alpha_StorageImport.this, "Failed to download file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("AudioPlayer", "Download failed", e);
                    }
                });
    }

    private void playAudio(String filePath) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepare();
            mediaPlayer.start();
            Toast.makeText(this, "Playing audio...", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e("AudioPlayer", "Playback failed", e);
            Toast.makeText(this, "Failed to play audio", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
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
            startActivity(new Intent(Alpha_StorageImport.this, MainActivity.class));
            return true;
        } else if (id == R.id.menuSignUp) {
            startActivity(new Intent(Alpha_StorageImport.this, SignupPage.class));
            return true;
        } else if (id == R.id.menuRecordPage) {
            startActivity(new Intent(Alpha_StorageImport.this, RecordPage.class));
            return true;
        } else if (id == R.id.menuRecordList) {
            startActivity(new Intent(Alpha_StorageImport.this, RecordsList.class));
            return true;
        } else if (id == R.id.menuAlphaBtnRecord) {
            startActivity(new Intent(Alpha_StorageImport.this, Alpha_BtnRecord.class));
            return true;
        } else if (id == R.id.menuAlphaChooseFile) {
            startActivity(new Intent(Alpha_StorageImport.this, Alpha_ChooseFile.class));
            return true;
        } else if (id == R.id.menuStorageImport) {
            // Already in Storage Import activity; no need for action here
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
}