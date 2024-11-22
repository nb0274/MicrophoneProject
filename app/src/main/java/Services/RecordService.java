package Services;

import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;

public class RecordService extends Service{

    MediaRecorder mRecorder;
    long mStartTime = 0;
    long mElapsedTime = 0;
    File mFile;
    String mFileName;

    public void onCreate(){
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        startRecording();
        return START_STICKY;
    }

    private void startRecording() {
        long tsLong = System.currentTimeMillis()/1000;
        String ts = String.valueOf(tsLong);

        mFileName = "audio_"+ts;
        mFile = new File(Environment.getExternalStorageDirectory() + "/MySoundRec");

        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRecorder.setOutputFile(mFile.getAbsolutePath());
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mRecorder.setAudioChannels(1);

        try{
            mRecorder.prepare();
            mRecorder.start();

            mStartTime = System.currentTimeMillis();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void stopRecording()
    {
        mRecorder.stop();
        mElapsedTime = (System.currentTimeMillis() - mStartTime);
        mRecorder.release();
        Toast.makeText(getApplicationContext(),"Recording saved "+mFile.getAbsolutePath(),Toast.LENGTH_LONG).show();

        // add to storage
    }

    public void onDestroy(){
        if(mRecorder != null)
        {
            stopRecording();
        }
        super.onDestroy();
    }
}
