package com.example.microphoneproject;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.protobuf.ByteString;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collections;

public class ConvertTextActivity extends AppCompatActivity {
    private static final String TAG = "ConvertTextActivity";
    private static final int PERMISSION_REQUEST_CODE = 101;
    private TextView txtTranscription;
    private String rid;
    private File audioFile, textFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_convert_text);

        txtTranscription = findViewById(R.id.txtTranscription);
        rid = getIntent().getStringExtra("rid");

        if (rid == null) {
            Toast.makeText(this, "Error: No recording ID found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Request necessary permissions
        if (!checkPermissions()) {
            requestPermissions();
        } else {
            checkIfTextExists();
        }
    }

    private void checkIfTextExists() {
        if (rid == null || rid.isEmpty()) {
            showToast("Error: Recording ID is missing.");
            return;  // Stop execution to prevent crash
        }

        StorageReference textRef = FirebaseStorage.getInstance().getReference("transcriptions/" + rid + ".txt");

        textRef.getDownloadUrl()
                .addOnSuccessListener(uri -> downloadAndShowText(uri))
                .addOnFailureListener(e -> downloadAudioFile());
    }

    private void downloadAndShowText(Uri uri) {
        new Thread(() -> {
            try {
                InputStream inputStream = new java.net.URL(uri.toString()).openStream();
                StringBuilder result = new StringBuilder();
                int data;
                while ((data = inputStream.read()) != -1) {
                    result.append((char) data);
                }
                inputStream.close();

                runOnUiThread(() -> txtTranscription.setText(result.toString()));
            } catch (IOException e) {
                e.printStackTrace();
                showToast("Failed to download transcription");
            }
        }).start();
    }


    /** 📥 Download MP4 audio from Firebase */
    private void downloadAudioFile() {
        StorageReference audioRef = FirebaseStorage.getInstance().getReference("audio_files/" + rid + ".mp4");
        audioFile = new File(getCacheDir(), rid + ".mp4");

        audioRef.getFile(audioFile)
                .addOnSuccessListener(taskSnapshot -> extractAudioData())
                .addOnFailureListener(e -> showToast("Failed to download audio file"));
    }

    /** 🎛 Extract raw audio data using MediaExtractor & MediaCodec */
    private void extractAudioData() {
        try {
            MediaExtractor extractor = new MediaExtractor();
            extractor.setDataSource(audioFile.getAbsolutePath());

            int trackIndex = -1;
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("audio/")) {
                    trackIndex = i;
                    extractor.selectTrack(i);
                    break;
                }
            }

            if (trackIndex == -1) {
                showToast("No audio track found in file.");
                return;
            }

            MediaFormat format = extractor.getTrackFormat(trackIndex);
            int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            int channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

            MediaCodec codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));
            codec.configure(format, null, null, 0);
            codec.start();

            ByteBuffer[] inputBuffers = codec.getInputBuffers();
            ByteBuffer[] outputBuffers = codec.getOutputBuffers();

            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            boolean isEOS = false;

            while (!isEOS) {
                int inIndex = codec.dequeueInputBuffer(10000);
                if (inIndex >= 0) {
                    ByteBuffer buffer = inputBuffers[inIndex];
                    int sampleSize = extractor.readSampleData(buffer, 0);
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        isEOS = true;
                    } else {
                        codec.queueInputBuffer(inIndex, 0, sampleSize, extractor.getSampleTime(), 0);
                        extractor.advance();
                    }
                }

                int outIndex = codec.dequeueOutputBuffer(info, 10000);
                if (outIndex >= 0) {
                    ByteBuffer outputBuffer = outputBuffers[outIndex];
                    byte[] pcmData = new byte[info.size];
                    outputBuffer.get(pcmData);
                    processAudioForTranscription(pcmData, sampleRate, channelCount);
                    codec.releaseOutputBuffer(outIndex, false);
                }
            }

            codec.stop();
            codec.release();
            extractor.release();

        } catch (IOException e) {
            e.printStackTrace();
            showToast("Error extracting audio");
        }
    }

    /** 🎙 Process raw audio and send to Google Speech API */
    private void processAudioForTranscription(byte[] pcmData, int sampleRate, int channelCount) {
        new Thread(() -> {
            try {
                GoogleCredentials credentials = loadCredentials();
                credentials
                        .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));
                SpeechSettings settings = SpeechSettings.newBuilder()
                        .setCredentialsProvider(() -> credentials)
                        .build();
                SpeechClient speechClient = SpeechClient.create(settings);

                RecognitionConfig config = RecognitionConfig.newBuilder()
                        .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)  // PCM format
                        .setSampleRateHertz(16000)  // Ensure correct sample rate
                        .setLanguageCode("en-US")
                        .build();

                ByteString audioBytes = ByteString.copyFrom(pcmData);
                RecognitionAudio audio = RecognitionAudio.newBuilder().setContent(audioBytes).build();

                RecognizeResponse response = speechClient.recognize(config, audio);
                speechClient.close();

                StringBuilder transcription = new StringBuilder();
                for (SpeechRecognitionResult result : response.getResultsList()) {
                    transcription.append(result.getAlternatives(0).getTranscript()).append(" ");
                }

                runOnUiThread(() -> {
                    txtTranscription.setText(transcription.toString());
                    saveTranscriptionToFirebase(transcription.toString());
                });

            } catch (Exception e) {
                e.printStackTrace();
                showToast("Transcription failed");
            }
        }).start();
    }

    private GoogleCredentials loadCredentials() throws IOException {
        File credentialsFile = new File(getFilesDir(), "speech_credentials.json");
        if (!credentialsFile.exists()) {
            showToast("Speech credentials file not found!");
            throw new IOException("Credentials file missing!");
        }
        return GoogleCredentials.fromStream(new FileInputStream(credentialsFile))
                .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));
    }



    /** 💾 Save transcription to Firebase */
    private void saveTranscriptionToFirebase(String transcription) {
        textFile = new File(getCacheDir(), rid + ".txt");

        try {
            FileOutputStream fos = new FileOutputStream(textFile);
            fos.write(transcription.getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            showToast("Error saving transcription");
            return;
        }

        StorageReference textRef = FirebaseStorage.getInstance().getReference("transcriptions/" + rid + ".txt");

        textRef.putFile(Uri.fromFile(textFile))
                .addOnSuccessListener(taskSnapshot -> showToast("Transcription saved"))
                .addOnFailureListener(e -> showToast("Failed to save transcription"));
    }

    /** 📜 Handle permissions */
    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
    }

    /** 🛠 Utility methods */
    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }
}
