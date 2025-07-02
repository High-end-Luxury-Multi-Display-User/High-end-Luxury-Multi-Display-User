package org.vosk.demo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class VoskActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private ImageView micIcon;
    private boolean isServiceRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        micIcon = findViewById(R.id.mic_icon);
        micIcon.setOnClickListener(v -> toggleService());

        checkPermissions();
        startServiceOnlyOnce(); // Start service to load model
    }

    private void toggleService() {
        Intent intent = new Intent(this, VoskRecognitionService.class);
        if (isServiceRunning) {
            intent.setAction(VoskRecognitionService.ACTION_STOP_RECOGNITION);
            startService(intent);
            Toast.makeText(this, "Stopped listening", Toast.LENGTH_SHORT).show();
        } else {
            intent.setAction(VoskRecognitionService.ACTION_START_RECOGNITION);
            startService(intent);
            Toast.makeText(this, "Started listening", Toast.LENGTH_SHORT).show();
        }
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
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO &&
                grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startServiceOnlyOnce();
        } else {
            Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT).show();
        }
    }
}
