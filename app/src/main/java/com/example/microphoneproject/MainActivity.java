package com.example.microphoneproject;

import static com.example.microphoneproject.FBRef.refAuth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseUser;

import Objects.User;

public class MainActivity extends AppCompatActivity {

    Intent si;
    EditText etEmail;
    EditText etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etEmail = findViewById(R.id.editTextUserEmail);
        etPassword = findViewById(R.id.editTextUserPassword);
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = refAuth.getCurrentUser();
        if (currentUser != null) {
            // Initialize the User singleton with current Firebase user details
            User user = User.getInstance();
            user.setUID(currentUser.getUid());
            user.setUsername(currentUser.getDisplayName()); // Ensure displayName is set in Firebase

            Toast.makeText(this, "Welcome back, " + user.getUsername(), Toast.LENGTH_SHORT).show();

            // Redirect to the main app screen (RecordPage)
            startActivity(new Intent(MainActivity.this, RecordPage.class));
            finish(); // Prevents going back to login screen on pressing back
        }
    }


    public void loginUser(View view) {
        String email = etEmail.getText().toString();
        String password = etPassword.getText().toString();

        refAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            FirebaseUser firebaseUser = refAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                User user = User.getInstance();
                                user.setUID(firebaseUser.getUid());
                                user.setUsername(firebaseUser.getDisplayName()); // Make sure username is set in Firebase Auth

                                Toast.makeText(MainActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(MainActivity.this, RecordPage.class)); // Navigate to main screen
                                finish();
                            }
                        }
                        else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(MainActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                            //updateUI(null);
                        }
                    }
                });
    }

    public void createUser(View view) {
        si = new Intent(this, SignupPage.class);
        startActivity(si);
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
            // Already in Log In activity; no need for action here
            return true;
        } else if (id == R.id.menuSignUp) {
            startActivity(new Intent(MainActivity.this, SignupPage.class));
            return true;
        } else if (id == R.id.menuRecordPage) {
            startActivity(new Intent(MainActivity.this, RecordPage.class));
            return true;
        } else if (id == R.id.menuRecordList) {
            startActivity(new Intent(MainActivity.this, RecordsList.class));
            return true;
        } else if (id == R.id.menuAlphaBtnRecord) {
            startActivity(new Intent(MainActivity.this, Alpha_BtnRecord.class));
            return true;
        } else if (id == R.id.menuAlphaChooseFile) {
            startActivity(new Intent(MainActivity.this, Alpha_ChooseFile.class));
            return true;
        } else if (id == R.id.menuStorageImport) {
            startActivity(new Intent(MainActivity.this, Alpha_StorageImport.class));
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
}