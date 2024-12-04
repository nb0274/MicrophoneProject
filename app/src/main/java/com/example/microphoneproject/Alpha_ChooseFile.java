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

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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
    private static final int REQUEST_STORAGE_PERMISSION_CODE = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alpha_choose_file);
        checkPermissions();


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
            StorageReference storageRef = storageReference.child("audio_files/" + file.getName());

            UploadTask uploadTask = storageRef.putStream(inputStream);

            // Using anonymous inner classes for listeners
            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // file uploaded successfully
                    Toast.makeText(Alpha_ChooseFile.this, "file uploaded successfully", Toast.LENGTH_SHORT).show();
                    // plays the file
                    playAudioFromFirebase(storageRef);
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

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION_CODE
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Storage Permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Storage Permission Denied", Toast.LENGTH_SHORT).show();
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
            startActivity(new Intent(Alpha_ChooseFile.this, MainActivity.class));
            return true;
        } else if (id == R.id.menuSignUp) {
            startActivity(new Intent(Alpha_ChooseFile.this, SignupPage.class));
            return true;
        } else if (id == R.id.menuRecordPage) {
            startActivity(new Intent(Alpha_ChooseFile.this, RecordPage.class));
            return true;
        } else if (id == R.id.menuRecordList) {
            startActivity(new Intent(Alpha_ChooseFile.this, RecordsList.class));
            return true;
        } else if (id == R.id.menuAlphaBtnRecord) {
            startActivity(new Intent(Alpha_ChooseFile.this, Alpha_BtnRecord.class));
            return true;
        } else if (id == R.id.menuAlphaChooseFile) {
            // Already in Choose File activity; no need for action here
            return true;
        } else if (id == R.id.menuStorageImport) {
            startActivity(new Intent(Alpha_ChooseFile.this, Alpha_StorageImport.class));
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

}