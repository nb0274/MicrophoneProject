package com.example.microphoneproject;
//l
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import Objects.Record;
import Objects.User;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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

    private ListView recordingsListView;
    private ArrayAdapter<String> adapter;
    private ArrayList<Record> recordingsList = new ArrayList<>();
    private ArrayList<String> displayList = new ArrayList<>();

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

        recordingsListView = findViewById(R.id.recordingsListView);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayList);
        recordingsListView.setAdapter(adapter);

        loadUserRecordings(); // Load recordings into the list

        recordingsListView.setOnItemLongClickListener((parent, view, position, id) -> {
            Record selectedRecord = recordingsList.get(position);
            showRecordOptionsDialog(selectedRecord, position);
            return true;
        });
    }

    /**
     * Handles a click of a record button, to start audio recording.
     * <p>
     * This method delegates the action of starting the recording process to the
     * {@code startRecording()} method.
     *
     * @param view The view that was clicked to trigger the recording (e.g., a record button).
     */
    public void onRecordClicked(View view) {
        startRecording();
    }

    /**
     * Handles a click of a pause/continue button, to toggle the recording state.
     * <p>
     * This method delegates the action of pausing or resuming the recording
     * to the {@code togglePauseContinue()} method.
     *
     * @param view The view that was clicked to trigger the pause/continue action (e.g., a button).
     */
    public void onPauseContinueClicked(View view) {
        togglePauseContinue();
    }

    /**
     * Handles a click of a cancel button, to stop and discard the current recording.
     * <p>
     * This method delegates the action of canceling the ongoing recording
     * to the {@code cancelRecording()} method.
     *
     * @param view The view that was clicked to trigger the cancel action (e.g., a cancel button).
     */
    public void onCancelClicked(View view) {
        cancelRecording();
    }

    /**
     * Handles a click event of a "done" button, to finalize the current recording.
     * <p>
     * This method delegates the action of completing and saving the ongoing recording
     * to the {@code finishRecording()} method.
     *
     * @param view The view that was clicked to trigger the finalization of the recording (e.g., a done button).
     */
    public void onDoneClicked(View view) {
        finishRecording();
    }

    /**
     * Initializes and starts an audio recording using {@link MediaRecorder}.
     * <p>
     * This method configures the MediaRecorder with an audio source (microphone), output format (MPEG_4),
     * output file path, and audio encoder (AAC). It then prepares and starts the recorder.
     * It also updates the UI button states to reflect the recording state, records the start time,
     * resets pause-related timers, and displays a toast message. Handles potential IOExceptions
     * during preparation or start.
     */
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

    /**
     * Toggles the recording state between paused and resumed for Android N (API 24) and above.
     * <p>
     * If the recording is currently paused, it resumes the {@link MediaRecorder}, updates the
     * pause/continue button text to "Pause", calculates the time spent paused, and sets
     * {@code isPaused} to false.
     * If the recording is active, it pauses the MediaRecorder, updates the button text to "Continue",
     * records the time when pausing started, and sets {@code isPaused} to true.
     * For devices below Android N, it shows a toast indicating that pause/continue is not supported.
     */
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

    /**
     * Stops the current recording, releases {@link MediaRecorder} resources, and deletes the recorded audio file.
     * <p>
     * If a {@code mediaRecorder} instance exists, it is stopped and released.
     * The UI buttons are reset to their initial state via {@code resetButtons()}.
     * The audio file specified by {@code audioFilePath} is deleted if it exists.
     * Finally, a toast message confirms the cancellation.
     */
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

    /**
     * Finalizes the current audio recording, prompts the user to save it, and then processes the save operation.
     * <p>
     * This method first stops and releases the {@link MediaRecorder}, ensuring it's resumed if paused (for API 24+).
     * It calculates the recording duration, then displays an {@link AlertDialog} to get a name for the recording.
     * If a name is provided, it delegates to {@code checkAndSaveRecording()} to handle the saving process.
     * Finally, it resets the UI button states via {@code resetButtons()}.
     * It includes error handling for stopping the MediaRecorder.
     */
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

    /**
     * Checks if a recording with the given name already exists for the current user in Firebase.
     * <p>
     * If a recording with the same name does not exist, it proceeds to upload the audio file
     * by calling {@code uploadAudioFile()}. If a recording with the same name exists,
     * a toast message is displayed to the user. It also handles potential failures
     * when trying to fetch existing recording names from Firebase.
     *
     * @param rName    The desired name for the new recording.
     * @param duration The duration of the recording in seconds.
     */
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

    /**
     * Uploads the recorded audio file to Firebase Storage and saves its metadata to the Firebase Realtime Database.
     * <p>
     * First, it checks if the local audio file exists. If not, it shows a toast and returns.
     * It generates a unique recording ID (RID) and creates a {@link StorageReference} to Firebase Storage.
     * The file is then uploaded using {@code putFile()}.
     * During the upload, progress is displayed via a toast message.
     * On successful upload, a {@link java.lang.Record} object containing metadata (duration, RID, UID, recording name)
     * is created and saved to the Firebase Realtime Database under the user's ID and the new RID.
     * If saving metadata is successful, a success toast is shown and the local audio file is deleted.
     * Failures during upload or metadata saving are communicated via toast messages.
     *
     * @param uid      The unique ID of the user.
     * @param rName    The name of the recording.
     * @param duration The duration of the recording in seconds.
     */
    private void uploadAudioFile(String uid, String rName, double duration) {
        File file = new File(audioFilePath);
        if (!file.exists()) {
            Toast.makeText(this, "No audio file found!", Toast.LENGTH_SHORT).show();
            return;
        }

        String rid = UUID.randomUUID().toString();
        StorageReference storageRef = FirebaseStorage.getInstance().getReference("audio_files/" + rid + ".mp4");

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

    /**
     * Resets the state of UI buttons and recording flags to their initial, non-recording state.
     * <p>
     * This method enables the record button and disables the pause/continue, cancel, and done buttons.
     * It also sets the text of the pause/continue button back to "Pause" and resets
     * the {@code isRecording} and {@code isPaused} boolean flags to false.
     */
    private void resetButtons() {
        btnRecord.setEnabled(true);
        btnPauseContinue.setEnabled(false);
        btnCancel.setEnabled(false);
        btnDone.setEnabled(false);
        btnPauseContinue.setText("Pause");
        isRecording = false;
        isPaused = false;
    }

    /**
     * Checks if the app has the {@link Manifest.permission#RECORD_AUDIO} permission.
     * <p>
     * If the permission has not been granted, it requests the permission from the user.
     * The result of the permission request is handled in {@code onRequestPermissionsResult}.
     */
    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
        }
    }

    /**
     * Initializes the contents of the Activity's standard options menu.
     * <p>
     * This method inflates the menu resource (defined in {@code R.menu.main}) into the
     * {@link Menu} object. This is called only once, the first time the options menu is displayed.
     *
     * @param menu The options menu in which items are placed.
     * @return You must return true for the menu to be displayed; if you return false it will not be shown.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu); // Replace with your menu file name if different
        return true;
    }

    /**
     * Loads and displays the current user's recordings from Firebase Realtime Database.
     * <p>
     * It attaches a {@link ValueEventListener} to the user's recordings path in Firebase.
     * On data change, it clears existing local lists ({@code recordingsList} and {@code displayList}),
     * then iterates through the retrieved {@link DataSnapshot}s. For each recording, it deserializes
     * it into a {@link java.lang.Record} object, sets its unique ID (RID) from the snapshot key,
     * and adds the Record object and a display string (name and duration) to the respective lists.
     * Finally, it notifies the {@code adapter} to refresh the ListView.
     * If data loading is cancelled or fails, a toast message is shown.
     */
    private void loadUserRecordings() {
        String uid = user.getUID();
        FBRef.refRecordings.child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                recordingsList.clear();
                displayList.clear();

                for (DataSnapshot recordSnapshot : snapshot.getChildren()) {
                    Record record = recordSnapshot.getValue(Record.class);

                    if (record != null) {
                        record.setRid(recordSnapshot.getKey());

                        recordingsList.add(record);
                        displayList.add(record.getRname() + " (" + record.getDuration() + " sec)");
                    }
                }

                adapter.notifyDataSetChanged(); // Update ListView
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(RecordPage.this, "Failed to load recordings.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Displays an {@link AlertDialog} with options for a selected recording.
     * <p>
     * The dialog presents the user with options: "Play", "Rename", "Delete", and "Convert to Text".
     * Based on the user's selection, it calls the corresponding method:
     * <ul>
     *     <li>"Play": {@code playRecording(record)}</li>
     *     <li>"Rename": {@code showRenameDialog(record, position)}</li>
     *     <li>"Delete": {@code deleteRecording(record, position)}</li>
     *     <li>"Convert to Text": {@code openConvertTextActivity(record)}</li>
     * </ul>
     *
     * @param record   The {@link java.lang.Record} object for which to show options.
     * @param position The position of the selected recording in the list, used for rename/delete operations.
     */
    private void showRecordOptionsDialog(Record record, int position) {
        final String[] options = {"Play", "Rename", "Delete", "Convert to Text"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Action")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Play
                            playRecording(record);
                            break;
                        case 1:
                            showRenameDialog(record, position);
                            break;
                        case 2:
                            deleteRecording(record, position);
                            break;
                        case 3:
                            openConvertTextActivity(record);
                            break;
                    }
                });
        builder.show();
    }

    /**
     * Displays an {@link AlertDialog} to allow the user to rename a recording.
     * <p>
     * The dialog includes an {@link EditText} field for the new name.
     * If the user enters a non-empty name and clicks "Rename", it calls
     * {@code checkAndRename(record, newName, position)} to process the renaming.
     * If the name is empty, a toast message is shown.
     * The dialog also has a "Cancel" button.
     *
     * @param record   The {@link java.lang.Record} object to be renamed.
     * @param position The position of the recording in the list, passed to {@code checkAndRename}.
     */
    private void showRenameDialog(Record record, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rename Recording");

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("Rename", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                checkAndRename(record, newName, position);
            } else {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    /**
     * Checks for recording ID validity and name uniqueness, then renames a recording in Firebase and updates local data.
     * <p>
     * It first validates the recording ID (RID). If invalid, it shows an error toast and returns.
     * It then checks if the new name is the same as the current name (case-insensitive, trimmed); if so, it shows a toast and returns.
     * Next, it queries Firebase to see if another recording by the current user already has the proposed new name.
     * If the new name already exists, a toast is shown.
     * Otherwise, it updates the 'rname' field for the recording in Firebase Realtime Database.
     * On successful database update, it updates the local {@link java.lang.Record} object, the corresponding entry in
     * {@code displayList}, notifies the {@code adapter} to refresh the ListView, and shows a success toast.
     * Failures during database operations are communicated via toast messages.
     *
     * @param record   The {@link java.lang.Record} object to be renamed.
     * @param newName  The proposed new name for the recording.
     * @param position The position of the recording in the {@code displayList} for UI update.
     */
    private void checkAndRename(Record record, String newName, int position) {
        String uid = user.getUID();
        String rid = record.getRid();  // Get the existing record ID

        if (rid == null || rid.isEmpty()) {
            Toast.makeText(RecordPage.this, "Error: Invalid recording ID.", Toast.LENGTH_SHORT).show();
            return;
        }

        newName = newName.trim(); // Trim whitespace
        if (newName.equalsIgnoreCase(record.getRname().trim())) {
            Toast.makeText(RecordPage.this, "New name is the same as the current name.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if the new name already exists
        String finalNewName = newName;
        FBRef.refRecordings.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean exists = false;
                for (DataSnapshot child : snapshot.getChildren()) {
                    String existingName = child.child("rname").getValue(String.class);
                    if (existingName != null && existingName.trim().equalsIgnoreCase(finalNewName)) {
                        exists = true;
                        break;
                    }
                }

                if (exists) {
                    Toast.makeText(RecordPage.this, "Recording name already exists.", Toast.LENGTH_SHORT).show();
                } else {
                    // Rename in the database
                    FBRef.refRecordings.child(uid).child(rid).child("rname").setValue(finalNewName)
                            .addOnSuccessListener(aVoid -> {
                                record.setRname(finalNewName); // Update local object
                                displayList.set(position, finalNewName + " (" + record.getDuration() + " sec)");
                                adapter.notifyDataSetChanged(); // Refresh ListView
                                Toast.makeText(RecordPage.this, "Renamed successfully.", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(RecordPage.this, "Rename failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(RecordPage.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Deletes a specified recording from Firebase Storage and its metadata from Firebase Realtime Database.
     * <p>
     * It first validates the provided {@link java.lang.Record} object and its ID (RID). If invalid, an error toast is shown.
     * It then attempts to delete the corresponding audio file (e.g., "audio_files/rid.mp4") from Firebase Storage.
     * <ul>
     *     <li>If storage deletion is successful, it proceeds to remove the recording's metadata
     *         from the Firebase Realtime Database (at path {@code /recordings/uid/rid}).</li>
     *     <li>If database deletion is also successful, it removes the recording from local lists
     *         ({@code recordingsList} and {@code displayList}) if the provided {@code position} is valid.
     *         It then calls {@code adapter.notifyDataSetChanged()} and {@code loadUserRecordings()}
     *         to refresh the displayed list from Firebase, and shows a success toast.</li>
     *     <li>Failures at any stage (storage deletion, database deletion) are communicated via toast messages.</li>
     * </ul>
     *
     * @param record   The {@link java.lang.Record} object representing the recording to be deleted.
     * @param position The position of the recording in the local lists, used for removal and UI update.
     */
    private void deleteRecording(Record record, int position) {
        String uid = user.getUID();

        if (record == null || record.getRid() == null) {
            Toast.makeText(this, "Error: Invalid recording ID.", Toast.LENGTH_SHORT).show();
            return;
        }

        String rid = record.getRid();
        StorageReference storageRef = FirebaseStorage.getInstance().getReference("audio_files/" + rid + ".mp4");

        storageRef.delete()
                .addOnSuccessListener(aVoid -> {
                    // Remove from Firebase Database
                    FBRef.refRecordings.child(uid).child(rid).removeValue()
                            .addOnSuccessListener(aVoid1 -> {
                                // Ensure valid position before removing from list
                                if (position >= 0 && position < recordingsList.size()) {
                                    recordingsList.remove(position);
                                    displayList.remove(position);
                                }

                                // **Force refresh the list by clearing and reloading**
                                adapter.notifyDataSetChanged();
                                loadUserRecordings(); // Refresh list from Firebase

                                Toast.makeText(this, "Deleted successfully.", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to delete from database: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete from storage: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Starts {@link PlayRecordingActivity} to play the selected recording.
     * <p>
     * It creates an {@link Intent} to launch {@code PlayRecordingActivity} and passes the
     * recording ID (RID) of the given {@link java.lang.Record} object as an extra with the key "RECORD_ID".
     *
     * @param record The {@link java.lang.Record} object whose recording is to be played.
     */
    private void playRecording(Record record) {
        Intent intent = new Intent(RecordPage.this, PlayRecordingActivity.class);
        intent.putExtra("RECORD_ID", record.getRid());  // Pass only the rid
        startActivity(intent);
    }

    /**
     * Opens the {@link ConvertTextActivity} to process the selected recording for speech-to-text conversion.
     * <p>
     * It creates an {@link Intent} to launch {@code ConvertTextActivity} and passes the
     * recording ID (RID) of the given {@link java.lang.Record} object as an extra with the key "rid".
     *
     * @param record The {@link java.lang.Record} object whose recording is to be converted to text.
     */
    private void openConvertTextActivity(Record record) {
        Intent intent = new Intent(this, ConvertTextActivity.class);
        intent.putExtra("rid", record.getRid()); // Pass the record ID
        startActivity(intent);
    }
}