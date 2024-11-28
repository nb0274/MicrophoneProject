package com.example.microphoneproject;

import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class Alpha_ChooseFile extends AppCompatActivity {

    private static final int PICK_AUDIO_FILE = 1; // audio pick code
    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alpha_choose_file);


        // Firebase Storage
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();

    }

    // opens file explorer
    public void openFilePicker(View view) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
        intent.setType("audio/*");
        startActivityForResult(intent, PICK_AUDIO_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_AUDIO_FILE && resultCode == RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            if (fileUri != null) {
                // uploads file to storage
                uploadFileToFirebase(fileUri);
            }
        }
    }

    // upload to Firebase Storage
    private void uploadFileToFirebase(Uri fileUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(fileUri);
            File file = new File(fileUri.getPath());
            StorageReference fileRef = storageReference.child("audio_files/" + file.getName());

            UploadTask uploadTask = fileRef.putStream(inputStream);

            // Using anonymous inner classes for listeners
            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // file uploaded successfully
                    Toast.makeText(Alpha_ChooseFile.this, "file uploaded successfully", Toast.LENGTH_SHORT).show();
                    // plays the file
                    playAudioFromFirebase(fileRef);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    // file couldn't upload
                    Toast.makeText(Alpha_ChooseFile.this, "file couldnt upload", Toast.LENGTH_SHORT).show();
                    Log.e("UploadError", e.getMessage(), e);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // plays from storage
    private void playAudioFromFirebase(StorageReference fileRef) {
        fileRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                // plays audio
                MediaPlayer mediaPlayer = new MediaPlayer();
                try {
                    mediaPlayer.setDataSource(uri.toString());
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(Alpha_ChooseFile.this, "couldn't download file", Toast.LENGTH_SHORT).show();
                Log.e("DownloadError", e.getMessage(), e);
            }
        });
    }


}