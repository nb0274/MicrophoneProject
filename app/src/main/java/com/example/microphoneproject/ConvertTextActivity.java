package com.example.microphoneproject;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
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
    private static final String BUCKET_NAME = "microphoneproject-91932.firebasestorage.app";
    private static final String CREDENTIALS_FILE = "speech_credentials.json";

    private TextView transcriptText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_convert_text);
        transcriptText = findViewById(R.id.txtTranscription);

        final String rid = getIntent().getStringExtra("rid");
        if (rid == null || rid.isEmpty()) {
            transcriptText.setText("Error: No recording ID provided");
            return;
        }

        transcriptText.setText("Transcribing, please wait...");

        new Thread(() -> {
            String resultTranscript = "";
            try {
                File mp4File = File.createTempFile("audio_" + rid, ".mp4", getCacheDir());
                StorageReference audioRef = FirebaseStorage.getInstance().getReference().child("audio_files/" + rid + ".mp4");
                Tasks.await(audioRef.getFile(mp4File));
                Log.d("TranscriptionActivity", "Downloaded audio to " + mp4File.getPath());

                File wavFile = new File(getCacheDir(), rid + ".wav");
                decodeMp4ToWav(mp4File.getAbsolutePath(), wavFile.getAbsolutePath());
                Log.d("TranscriptionActivity", "Converted to WAV: " + wavFile.getPath());

                InputStream credStream = getAssets().open(CREDENTIALS_FILE);
                GoogleCredentials credentials = GoogleCredentials.fromStream(credStream)
                        .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));
                credentials.refreshIfExpired();
                String token = credentials.getAccessToken().getTokenValue();
                uploadFileToGCS(wavFile, BUCKET_NAME, rid + ".wav", token);
                Log.d("TranscriptionActivity", "Uploaded WAV to GCS: gs://" + BUCKET_NAME + "/transcripts/" + rid + ".wav");

                SpeechSettings speechSettings = SpeechSettings.newBuilder()
                        .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                        .setTransportChannelProvider(SpeechSettings.defaultHttpJsonTransportProviderBuilder().build())
                        .build();
                try (SpeechClient speechClient = SpeechClient.create(speechSettings)) {
                    RecognitionConfig config = RecognitionConfig.newBuilder()
                            .setEncoding(AudioEncoding.LINEAR16)
                            .setSampleRateHertz(16000)
                            .setLanguageCode("en-US")
                            .build();

                    RecognitionAudio audio = RecognitionAudio.newBuilder()
                            .setUri("gs://" + BUCKET_NAME + "/transcripts/" + rid + ".wav")
                            .build();

                    RecognizeResponse response = speechClient.recognize(config, audio);
                    Log.d("ConvertTextActivity", "Full RecognizeResponse: " + response.toString());

                    StringBuilder sb = new StringBuilder();
                    for (SpeechRecognitionResult result : response.getResultsList()) {
                        if (result.getAlternativesCount() > 0) {
                            SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
                            sb.append(alternative.getTranscript()).append("\n");
                        }
                    }
                    resultTranscript = sb.toString().trim();

                    if (resultTranscript.isEmpty()) {
                        Log.w("ConvertTextActivity", "Transcription result is empty.");
                        runOnUiThread(() -> {
                            transcriptText.setText("");
                            Toast.makeText(ConvertTextActivity.this, "Transcription is empty", Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        String finalResultTranscript = resultTranscript;
                        runOnUiThread(() -> transcriptText.setText(finalResultTranscript));
                    }
                }
            } catch (Exception e) {
                Log.e("TranscriptionActivity", "Transcription failed", e);
                final String errorText = "Error: " + e.getMessage();
                runOnUiThread(() -> transcriptText.setText(errorText));
            }
        }).start();
    }

    private void decodeMp4ToWav(String mp4Path, String wavPath) throws IOException {
        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(mp4Path);

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
        if (audioTrackIndex < 0 || format == null) throw new IOException("No audio track found in the MP4 file.");
        extractor.selectTrack(audioTrackIndex);

        MediaCodec decoder = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));
        decoder.configure(format, null, null, 0);
        decoder.start();

        ByteBuffer[] inputBuffers = decoder.getInputBuffers();
        ByteBuffer[] outputBuffers = decoder.getOutputBuffers();
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

        FileOutputStream wavOut = new FileOutputStream(wavPath);
        writeWavHeader(new DataOutputStream(wavOut), 0, 16000, 1, 16);

        int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        boolean sawInputEOS = false, sawOutputEOS = false;
        int totalPcmBytes = 0;

        while (!sawOutputEOS) {
            if (!sawInputEOS) {
                int inputIndex = decoder.dequeueInputBuffer(10000);
                if (inputIndex >= 0) {
                    ByteBuffer buffer = inputBuffers[inputIndex];
                    int sampleSize = extractor.readSampleData(buffer, 0);
                    if (sampleSize < 0) {
                        decoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        sawInputEOS = true;
                    } else {
                        long presentationTimeUs = extractor.getSampleTime();
                        decoder.queueInputBuffer(inputIndex, 0, sampleSize, presentationTimeUs, 0);
                        extractor.advance();
                    }
                }
            }

            int outputIndex = decoder.dequeueOutputBuffer(info, 10000);
            if (outputIndex >= 0) {
                ByteBuffer outBuffer = outputBuffers[outputIndex];
                if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    decoder.releaseOutputBuffer(outputIndex, false);
                    continue;
                }
                if (info.size > 0) {
                    byte[] pcmData = new byte[info.size];
                    outBuffer.position(0);
                    outBuffer.get(pcmData);
                    outBuffer.clear();
                    if (channelCount == 2) {
                        pcmData = downmixToMono(pcmData);
                        channelCount = 1;
                    }
                    if (sampleRate != 16000) {
                        pcmData = resamplePCM(pcmData, sampleRate, 16000);
                        sampleRate = 16000;
                    }
                    wavOut.write(pcmData);
                    totalPcmBytes += pcmData.length;
                }
                decoder.releaseOutputBuffer(outputIndex, false);
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) sawOutputEOS = true;
            } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat newFormat = decoder.getOutputFormat();
                if (newFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE))
                    sampleRate = newFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                if (newFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT))
                    channelCount = newFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            } else if (outputIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                outputBuffers = decoder.getOutputBuffers();
            }
        }

        decoder.stop();
        decoder.release();
        extractor.release();
        wavOut.close();

        RandomAccessFile wavFile = new RandomAccessFile(wavPath, "rw");
        writeWavHeader(wavFile, totalPcmBytes, sampleRate, channelCount, 16);
        wavFile.close();
    }

    private void writeWavHeader(DataOutput out, int rawDataSize, int sampleRate, int channels, int bitsPerSample) throws IOException {
        out.writeBytes("RIFF");
        out.writeInt(Integer.reverseBytes(36 + rawDataSize));
        out.writeBytes("WAVE");
        out.writeBytes("fmt ");
        out.writeInt(Integer.reverseBytes(16));
        out.writeShort(Short.reverseBytes((short) 1));
        out.writeShort(Short.reverseBytes((short) channels));
        out.writeInt(Integer.reverseBytes(sampleRate));
        int byteRate = sampleRate * channels * (bitsPerSample / 8);
        out.writeInt(Integer.reverseBytes(byteRate));
        short blockAlign = (short) (channels * (bitsPerSample / 8));
        out.writeShort(Short.reverseBytes(blockAlign));
        out.writeShort(Short.reverseBytes((short) bitsPerSample));
        out.writeBytes("data");
        out.writeInt(Integer.reverseBytes(rawDataSize));
    }

    private byte[] downmixToMono(byte[] stereoData) {
        ByteBuffer buffer = ByteBuffer.wrap(stereoData).order(ByteOrder.LITTLE_ENDIAN);
        int totalSamples = stereoData.length / 2;
        int frameCount = totalSamples / 2;
        ByteBuffer monoBuffer = ByteBuffer.allocate(frameCount * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < frameCount; i++) {
            short left = buffer.getShort();
            short right = buffer.getShort();
            int mixed = (left + right) / 2;
            monoBuffer.putShort((short) mixed);
        }
        return monoBuffer.array();
    }

    private byte[] resamplePCM(byte[] pcmData, int srcSampleRate, int targetSampleRate) {
        if (srcSampleRate == targetSampleRate) return pcmData;
        ByteBuffer srcBuffer = ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN);
        int totalSamples = pcmData.length / 2;
        int outSampleCount = (int) (((long) totalSamples) * targetSampleRate / srcSampleRate);
        ByteBuffer outBuffer = ByteBuffer.allocate(outSampleCount * 2).order(ByteOrder.LITTLE_ENDIAN);
        ShortBuffer srcShorts = srcBuffer.asShortBuffer();
        for (int i = 0; i < outSampleCount; i++) {
            double srcIndex = i * (double) srcSampleRate / targetSampleRate;
            int indexInt = (int) Math.floor(srcIndex);
            double frac = srcIndex - indexInt;
            short s0 = srcShorts.get(Math.min(indexInt, totalSamples - 1));
            short s1 = srcShorts.get(Math.min(indexInt + 1, totalSamples - 1));
            double interpolated = s0 + (s1 - s0) * frac;
            outBuffer.putShort((short) Math.round(interpolated));
        }
        return outBuffer.array();
    }

    private void uploadFileToGCS(File file, String bucketName, String objectName, String oauthToken) throws IOException {
        FileInputStream inputStream = new FileInputStream(file);
        try {
            String urlStr = "https://storage.googleapis.com/upload/storage/v1/b/"
                    + URLEncoder.encode(bucketName, "UTF-8")
                    + "/o?uploadType=media&name=" + URLEncoder.encode("transcripts/" + objectName, "UTF-8");
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + oauthToken);
            conn.setRequestProperty("Content-Type", "audio/wav");

            OutputStream out = conn.getOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
            out.close();

            int responseCode = conn.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
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