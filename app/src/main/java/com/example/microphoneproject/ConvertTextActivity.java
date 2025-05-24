package com.example.microphoneproject;
//l
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognitionConfig.AudioEncoding;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.cloud.speech.v1.SpeechSettings;
import com.google.api.gax.core.FixedCredentialsProvider;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.BufferedWriter;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Collections;

public class ConvertTextActivity extends AppCompatActivity {
    // Update these constants with your Cloud Storage bucket name and credentials file name
    private static final String BUCKET_NAME = "microphoneproject-91932.firebasestorage.app";
    private static final String CREDENTIALS_FILE = "speech_credentials.json";

    private TextView transcriptText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_convert_text);  // layout containing a TextView for output
        transcriptText = findViewById(R.id.txtTranscription);

        // Get recording ID from Intent extras
        final String rid = getIntent().getStringExtra("rid");
        if (rid == null || rid.isEmpty()) {
            transcriptText.setText("Error: No recording ID provided");
            return;
        }

        // Indicate progress to the user
        transcriptText.setText("Transcribing, please wait...");

        // Run the transcription process in a background thread (to avoid blocking the UI)
        new Thread(() -> {
            String resultTranscript = "";
            try {
                // 1. Download the .mp4 audio file from Firebase Storage
                File mp4File = File.createTempFile("audio_" + rid, ".mp4", getCacheDir());
                StorageReference audioRef = FirebaseStorage.getInstance()
                        .getReference().child("audio_files/" + rid + ".mp4");
                // Use Tasks.await to synchronously download the file (running in background thread)
                Tasks.await(audioRef.getFile(mp4File));
                Log.d("TranscriptionActivity", "Downloaded audio to " + mp4File.getPath());

                // 2. Extract AAC audio from the MP4 and convert to a 16kHz PCM WAV file
                File wavFile = new File(getCacheDir(), rid + ".wav");
                decodeMp4ToWav(mp4File.getAbsolutePath(), wavFile.getAbsolutePath());
                Log.d("TranscriptionActivity", "Converted to WAV: " + wavFile.getPath());

                // 3. Upload the WAV file to Google Cloud Storage (for the Speech API to access)
                // Load Google Cloud credentials from JSON (service account key file in assets)
                InputStream credStream = getAssets().open(CREDENTIALS_FILE);
                GoogleCredentials credentials = GoogleCredentials.fromStream(credStream)
                        .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));
                credentials.refreshIfExpired();
                String token = credentials.getAccessToken().getTokenValue();
                uploadFileToGCS(wavFile, BUCKET_NAME, rid + ".wav", token);
                Log.d("TranscriptionActivity", "Uploaded WAV to GCS: gs://" + BUCKET_NAME + "/" + rid + ".wav");

                // 4. Use Google Cloud Speech-to-Text API (Java SDK, using REST) to transcribe the audio
                SpeechSettings speechSettings = SpeechSettings.newBuilder()
                        // Use the credentials and configure HTTP+JSON transport (no gRPC)
                        .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                        .setTransportChannelProvider(
                                SpeechSettings.defaultHttpJsonTransportProviderBuilder().build()
                        )
                        .build();
                try (SpeechClient speechClient = SpeechClient.create(speechSettings)) {
                    // Configure recognition parameters
                    RecognitionConfig config = RecognitionConfig.newBuilder()
                            .setEncoding(AudioEncoding.LINEAR16)
                            .setSampleRateHertz(16000)
                            .setLanguageCode("en-US")
                            .build();
                    // Specify the GCS URI of the uploaded audio
                    RecognitionAudio audio = RecognitionAudio.newBuilder()
                            .setUri("gs://" + BUCKET_NAME + "/transcripts" + "/" + rid + ".wav")
                            .build();
                    // Transcribe the audio file (synchronous recognition)
                    RecognizeResponse response = speechClient.recognize(config, audio);
                    // Concatenate transcription from all results
                    StringBuilder sb = new StringBuilder();
                    for (SpeechRecognitionResult result : response.getResultsList()) {
                        if (result.getAlternativesCount() > 0) {
                            SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
                            sb.append(alternative.getTranscript()).append("\n");
                        }
                    }
                    resultTranscript = sb.toString().trim();
                }
            } catch (Exception e) {
                Log.e("TranscriptionActivity", "Transcription failed", e);
                resultTranscript = "Error: " + e.getMessage();
            }

            // 5. Update the UI with the transcription result (on the main thread)
            final String outputText = resultTranscript;
            runOnUiThread(() -> transcriptText.setText(outputText));
        }).start();
    }

    /**
     * Extracts AAC audio from an MP4 file and writes it as a WAV (PCM 16-bit, 16 kHz, mono).
     */
    private void decodeMp4ToWav(String mp4Path, String wavPath) throws IOException {
        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(mp4Path);

        // Find the first audio track (AAC) in the MP4
        MediaFormat format = null;
        int audioTrackIndex = -1;
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat fmt = extractor.getTrackFormat(i);
            String mime = fmt.getString(MediaFormat.KEY_MIME);
            if (mime != null && mime.startsWith("audio/")) {
                format = fmt;
                audioTrackIndex = i;
                break;
            }
        }
        if (audioTrackIndex < 0 || format == null) {
            throw new IOException("No audio track found in the MP4 file.");
        }
        extractor.selectTrack(audioTrackIndex);

        // Set up the AAC decoder
        MediaCodec decoder = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));
        decoder.configure(format, null, null, 0);
        decoder.start();

        // Prepare buffers for decoding
        ByteBuffer[] inputBuffers = decoder.getInputBuffers();
        ByteBuffer[] outputBuffers = decoder.getOutputBuffers();
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

        // Prepare WAV file output stream and write a placeholder header (to be updated later)
        FileOutputStream wavOut = new FileOutputStream(wavPath);
        writeWavHeader(new DataOutputStream(wavOut), 0, 16000, 1, 16);// Placeholder header for WAV (will fix sizes later)

        int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        int totalPcmBytes = 0;

        // Decode loop: read from extractor, feed decoder, retrieve PCM output
        while (!sawOutputEOS) {
            // Feed input AAC bytes into the decoder
            if (!sawInputEOS) {
                int inputIndex = decoder.dequeueInputBuffer(10000);
                if (inputIndex >= 0) {
                    ByteBuffer buffer = inputBuffers[inputIndex];
                    int sampleSize = extractor.readSampleData(buffer, 0);
                    if (sampleSize < 0) {
                        // End of input stream
                        decoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        sawInputEOS = true;
                    } else {
                        long presentationTimeUs = extractor.getSampleTime();
                        decoder.queueInputBuffer(inputIndex, 0, sampleSize, presentationTimeUs, 0);
                        extractor.advance();
                    }
                }
            }

            // Get decoded output PCM bytes
            int outputIndex = decoder.dequeueOutputBuffer(info, 10000);
            if (outputIndex >= 0) {
                ByteBuffer outBuffer = outputBuffers[outputIndex];
                if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // Codec config packet, skip it
                    decoder.releaseOutputBuffer(outputIndex, false);
                    continue;
                }
                if (info.size > 0) {
                    // Copy the PCM data from the buffer
                    byte[] pcmData = new byte[info.size];
                    outBuffer.position(0);
                    outBuffer.get(pcmData);
                    outBuffer.clear();
                    // If stereo, downmix to mono for transcription
                    if (channelCount == 2) {
                        pcmData = downmixToMono(pcmData);
                        channelCount = 1;
                    }
                    // Resample to 16kHz if needed (Speech API works best at 16000 Hz)
                    if (sampleRate != 16000) {
                        pcmData = resamplePCM(pcmData, sampleRate, 16000);
                        sampleRate = 16000;
                    }
                    // Write PCM data to WAV file
                    wavOut.write(pcmData);
                    totalPcmBytes += pcmData.length;
                }
                decoder.releaseOutputBuffer(outputIndex, false);
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    sawOutputEOS = true;  // Reached end of decoding
                }
            } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // Update sample rate and channel count if the decoder output format changes (e.g., HE-AAC)
                MediaFormat newFormat = decoder.getOutputFormat();
                if (newFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                    sampleRate = newFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                }
                if (newFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                    channelCount = newFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                }
            } else if (outputIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                outputBuffers = decoder.getOutputBuffers();
            }
        }

        // Clean up decoder and extractor
        decoder.stop();
        decoder.release();
        extractor.release();
        wavOut.close();

        // Now rewrite the WAV header with the actual PCM data size, sample rate, etc.
        RandomAccessFile wavFile = new RandomAccessFile(wavPath, "rw");
        writeWavHeader(wavFile, totalPcmBytes, sampleRate, channelCount, 16);
        wavFile.close();
    }

    /**
     * Writes a WAV header to the given DataOutput (FileOutputStream or RandomAccessFile).
     * This should be called twice: once with placeholder values and once at the end to update sizes.
     */
    private void writeWavHeader(DataOutput out, int rawDataSize, int sampleRate, int channels, int bitsPerSample) throws IOException {
        // RIFF chunk descriptor
        out.writeBytes("RIFF");
        int fileLength = 36 + rawDataSize;  // 36 + data length (excluding RIFF chunk and size field)
        out.writeInt(Integer.reverseBytes(fileLength));       // Chunk size in little-endian
        out.writeBytes("WAVE");
        // fmt subchunk
        out.writeBytes("fmt ");
        out.writeInt(Integer.reverseBytes(16));               // Subchunk1 size (16 for PCM)
        out.writeShort(Short.reverseBytes((short) 1));        // Audio format (1 = PCM)
        out.writeShort(Short.reverseBytes((short) channels)); // Number of channels
        out.writeInt(Integer.reverseBytes(sampleRate));       // Sample rate
        int byteRate = sampleRate * channels * (bitsPerSample / 8);
        out.writeInt(Integer.reverseBytes(byteRate));         // Byte rate
        short blockAlign = (short) (channels * (bitsPerSample / 8));
        out.writeShort(Short.reverseBytes(blockAlign));       // Block align
        out.writeShort(Short.reverseBytes((short) bitsPerSample)); // Bits per sample
        // data subchunk
        out.writeBytes("data");
        out.writeInt(Integer.reverseBytes(rawDataSize));      // Subchunk2 size (data length)
    }

    /**
     * Downmixes interleaved stereo PCM 16-bit data to mono by averaging left and right channels.
     */
    private byte[] downmixToMono(byte[] stereoData) {
        // stereoData length should be even and represent 16-bit PCM frames [L, R]
        ByteBuffer buffer = ByteBuffer.wrap(stereoData).order(ByteOrder.LITTLE_ENDIAN);
        int totalSamples = stereoData.length / 2;            // number of 16-bit samples (both channels combined)
        int frameCount = totalSamples / 2;                   // number of stereo frames
        ByteBuffer monoBuffer = ByteBuffer.allocate(frameCount * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < frameCount; i++) {
            short left = buffer.getShort();
            short right = buffer.getShort();
            int mixed = (left + right) / 2;                  // simple average of left and right
            monoBuffer.putShort((short) mixed);
        }
        return monoBuffer.array();
    }

    /**
     * Resample 16-bit PCM audio from srcSampleRate to targetSampleRate (mono audio).
     * Uses linear interpolation. Input data is little-endian PCM.
     */
    private byte[] resamplePCM(byte[] pcmData, int srcSampleRate, int targetSampleRate) {
        if (srcSampleRate == targetSampleRate) {
            return pcmData;  // no resampling needed
        }
        ByteBuffer srcBuffer = ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN);
        int totalSamples = pcmData.length / 2;  // number of 16-bit samples in pcmData (mono)
        // Calculate length of output sample array
        int outSampleCount = (int) (((long) totalSamples) * targetSampleRate / srcSampleRate);
        ByteBuffer outBuffer = ByteBuffer.allocate(outSampleCount * 2).order(ByteOrder.LITTLE_ENDIAN);
        ShortBuffer srcShorts = srcBuffer.asShortBuffer();
        for (int i = 0; i < outSampleCount; i++) {
            double srcIndex = i * (double) srcSampleRate / targetSampleRate;
            int indexInt = (int) Math.floor(srcIndex);
            double frac = srcIndex - indexInt;
            // Get two adjacent samples for interpolation (clamp at ends)
            short s0 = srcShorts.get(Math.min(indexInt, totalSamples - 1));
            short s1 = srcShorts.get(Math.min(indexInt + 1, totalSamples - 1));
            // Linear interpolate between s0 and s1
            double interpolated = s0 + (s1 - s0) * frac;
            outBuffer.putShort((short) Math.round(interpolated));
        }
        return outBuffer.array();
    }

    /**
     * Uploads a file to Google Cloud Storage using an HTTP POST (requires an OAuth2 access token).
     */
    private void uploadFileToGCS(File file, String bucketName, String objectName, String oauthToken) throws IOException {
        FileInputStream inputStream = new FileInputStream(file);
        try {
            // Construct the GCS upload URL (using uploadType=media for direct upload)
            String urlStr = "https://storage.googleapis.com/upload/storage/v1/b/"
                    + URLEncoder.encode(bucketName, "UTF-8")
                    + "/o?uploadType=media&name=" + URLEncoder.encode("transcripts/" + objectName, "UTF-8");
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            // Set authorization header with Bearer token
            conn.setRequestProperty("Authorization", "Bearer " + oauthToken);
            // Set the content type for the file (WAV audio)
            conn.setRequestProperty("Content-Type", "audio/wav");

            // Write file data to request
            OutputStream out = conn.getOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
            out.close();

            // Check the response for success
            int responseCode = conn.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                // If upload failed, read the error message from the response
                InputStream errorStream = conn.getErrorStream();
                StringBuilder errorMsg = new StringBuilder();
                if (errorStream != null) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorMsg.append(line);
                    }
                    reader.close();
                }
                throw new IOException("GCS upload failed with HTTP " + responseCode + ": " + errorMsg);
            }
        } finally {
            inputStream.close();
        }
    }
}
