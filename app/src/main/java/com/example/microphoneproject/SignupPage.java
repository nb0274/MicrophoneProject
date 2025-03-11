package com.example.microphoneproject;

import static com.example.microphoneproject.FBRef.refAuth;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import Objects.User;

public class SignupPage extends AppCompatActivity {
    Context context;
    EditText etEmail;
    EditText etUsername;
    EditText etPassword;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;});

        etEmail = findViewById(R.id.editTextUserEmail);
        etUsername = findViewById(R.id.editTextUserName);
        etPassword = findViewById(R.id.editTextUserPassword);
        context = this;
        FirebaseApp.initializeApp(context);
    }




    public void createUser(View view) {
        String email = etEmail.getText().toString();
        String username = etUsername.getText().toString();
        String password = etPassword.getText().toString();

        if (email.isEmpty() || password.isEmpty() || username.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_LONG).show();
        } else {
            ProgressDialog pd = new ProgressDialog(this);
            pd.setTitle("Connecting");
            pd.setMessage("Creating user...");
            pd.show();

            refAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            pd.dismiss();

                            if (task.isSuccessful()) {
                                FirebaseUser firebaseUser = refAuth.getCurrentUser();
                                if (firebaseUser != null) {
                                    User user = User.getInstance();
                                    user.setUID(firebaseUser.getUid());
                                    user.setUsername(username);

                                    // Optionally update Firebase User profile with username
                                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                            .setDisplayName(username)
                                            .build();

                                    firebaseUser.updateProfile(profileUpdates);

                                    Toast.makeText(context, "User created successfully!", Toast.LENGTH_LONG).show();
                                    startActivity(new Intent(SignupPage.this, RecordPage.class)); // Navigate to main screen
                                    finish();
                                }
                            } else {
                                handleSignupError(task.getException());
                            }
                        }
                    });
        }
    }

    // Handles various signup errors
    private void handleSignupError(Exception exp) {
        if (exp instanceof FirebaseAuthInvalidUserException)
            Toast.makeText(context, "Invalid email address", Toast.LENGTH_LONG).show();
        else if (exp instanceof FirebaseAuthWeakPasswordException)
            Toast.makeText(context, "Password too weak", Toast.LENGTH_LONG).show();
        else if (exp instanceof FirebaseAuthUserCollisionException)
            Toast.makeText(context, "User already exists", Toast.LENGTH_LONG).show();
        else if (exp instanceof FirebaseAuthInvalidCredentialsException)
            Toast.makeText(context, "General authentication failure", Toast.LENGTH_LONG).show();
        else if (exp instanceof FirebaseNetworkException)
            Toast.makeText(context, "Network error. Please check your connection", Toast.LENGTH_LONG).show();
        else
            Toast.makeText(context, "An error occurred, please try again later", Toast.LENGTH_LONG).show();
    }


    /*
        gets you back to the login screen
     */
    public void loginUser(View view) {
        finish();
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
            startActivity(new Intent(SignupPage.this, MainActivity.class));
            return true;
        } else if (id == R.id.menuSignUp) {
            // Already in Sign Up activity; no need for action here
            return true;
        } else if (id == R.id.menuRecordPage) {
            startActivity(new Intent(SignupPage.this, RecordPage.class));
            return true;
        } else if (id == R.id.menuRecordList) {
            startActivity(new Intent(SignupPage.this, RecordsList.class));
            return true;
        } else if (id == R.id.menuAlphaBtnRecord) {
            startActivity(new Intent(SignupPage.this, Alpha_BtnRecord.class));
            return true;
        } else if (id == R.id.menuAlphaChooseFile) {
            startActivity(new Intent(SignupPage.this, Alpha_ChooseFile.class));
            return true;
        } else if (id == R.id.menuStorageImport) {
            startActivity(new Intent(SignupPage.this, Alpha_StorageImport.class));
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
}