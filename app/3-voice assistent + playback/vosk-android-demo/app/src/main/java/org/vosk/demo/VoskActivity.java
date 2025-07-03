package org.vosk.demo;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class VoskActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private ImageView micIcon, statusImage;
    private boolean isServiceRunning = false;


    private VoiceCommandReceiver commandReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        micIcon = findViewById(R.id.mic_icon);
        statusImage = findViewById(R.id.statusImage);

        micIcon.setOnClickListener(v -> toggleService());

        checkPermissions();
        startServiceOnlyOnce();
    }



    private void toggleService() {
        Intent intent = new Intent(this, VoskRecognitionService.class);
        if (isServiceRunning) {
            intent.setAction(VoskRecognitionService.ACTION_STOP_RECOGNITION);
        } else {
            intent.setAction(VoskRecognitionService.ACTION_START_RECOGNITION);
        }
        startService(intent);
        isServiceRunning = !isServiceRunning;
        updateMicIcon();
    }

    private void updateMicIcon() {
        micIcon.setImageResource(isServiceRunning ? R.drawable.ic_mic_on : R.drawable.ic_mic_off);
    }

    private void startServiceOnlyOnce() {
        Intent intent = new Intent(this, VoskRecognitionService.class);
        startService(intent);
        isServiceRunning = true;
        updateMicIcon();
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    PERMISSIONS_REQUEST_RECORD_AUDIO);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        commandReceiver = new VoiceCommandReceiver(this, statusImage);
        registerReceiver(commandReceiver, new IntentFilter("org.vosk.demo.ACTION_COMMAND"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (commandReceiver != null) {
            unregisterReceiver(commandReceiver);
            commandReceiver = null;
        }
    }

}
