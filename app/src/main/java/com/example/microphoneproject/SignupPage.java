package com.example.microphoneproject;
//l
import static com.example.microphoneproject.FBRef.refAuth;

import android.app.Activity;
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


    /**
     * Attempts to create a new user account using Firebase Authentication and save user details to Firebase Realtime Database.
     * <p>
     * This method is typically called when a user clicks a "sign up" button.
     * It retrieves the email, username, and password from the respective EditText fields.
     * If any field is empty, a toast message is displayed. Otherwise, a {@link ProgressDialog} is shown
     * while the user creation process is in progress.
     * <p>
     * It calls {@code refAuth.createUserWithEmailAndPassword()} to create the user in Firebase Authentication.
     * <ul>
     *     <li>If authentication is successful, it retrieves the new user's UID. A {@link User} object
     *         is then created with the UID, username, and password (note: storing plain text passwords
     *         is generally not recommended for production). This {@code User} object is then saved to
     *         the Firebase Realtime Database under {@code FBRef.refUsers.child(uid)}.</li>
     *     <li>If saving to the database is successful, a success toast is shown, an {@link Intent}
     *         with the email and password is prepared as a result for the calling activity,
     *         and this activity is finished.</li>
     *     <li>If saving to the database fails, an error toast is shown.</li>
     *     <li>If Firebase Authentication fails, {@code handleSignupError()} is called to display
     *         an appropriate error message.</li>
     * </ul>
     * The {@link ProgressDialog} is dismissed after the authentication attempt completes.
     *
     * @param view The view that triggered this method, typically a Button.
     */
    public void createUser(View view) {
        String email = etEmail.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

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
                                String uid = firebaseUser.getUid(); // Get the UID of the newly created user

                                User newUser = User.getInstance();
                                newUser.setUID(uid);
                                newUser.setUsername(username);
                                newUser.setPassword(password);

                                // Write user data to Firebase Database
                                FBRef.refUsers.child(uid).setValue(newUser)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> dbTask) {
                                                if (dbTask.isSuccessful()) {
                                                    Toast.makeText(context, "User created and saved successfully", Toast.LENGTH_LONG).show();

                                                    Intent resultIntent = new Intent();
                                                    resultIntent.putExtra("email", email);
                                                    resultIntent.putExtra("password", password);
                                                    setResult(Activity.RESULT_OK, resultIntent);
                                                    finish();
                                                }
                                                else {
                                                    Toast.makeText(context, "Failed to save user data", Toast.LENGTH_LONG).show();
                                                }
                                            }
                                        });
                            } else {
                                handleSignupError(task.getException());
                            }
                        }
                    });
        }
    }


    /**
     * Handles various exceptions that can occur during Firebase user signup and displays appropriate toast messages.
     * <p>
     * It checks the type of the provided {@link Exception} and shows a specific error message
     * to the user via a {@link Toast}. This includes errors like invalid email, weak password,
     * user collision (email already exists), invalid credentials, network issues, or a generic error.
     *
     * @param exp The exception caught during the signup process.
     */
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


    /**
     * Finishes the current activity, returning the user to the previous screen(login Screen).
     * <p>
     * This method is typically used as an onClick handler for a button that allows the user
     * to navigate back from the current signup page.
     *
     * @param view The view that triggered this method, typically a Button.
     */
    public void returnToLoginUser(View view) {
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
}