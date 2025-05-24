package com.example.microphoneproject;
//l
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FBRef {
    public static FirebaseAuth refAuth = FirebaseAuth.getInstance();
    public static FirebaseDatabase database = FirebaseDatabase.getInstance();
    public static DatabaseReference refUsers = database.getReference("Users");
    public static DatabaseReference refRecordings = database.getReference("Recordings");
}

