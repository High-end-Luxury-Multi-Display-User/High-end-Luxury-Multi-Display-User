package org.vosk.demo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import org.json.JSONException;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import org.vosk.android.StorageService;

import java.io.File;

public class VoskRecognitionService extends Service implements RecognitionListener {

    private static final String TAG = "VoskService";
    private static final int NOTIFICATION_ID = 1234;
    private static final String CHANNEL_ID = "vosk_channel";

    public static final String ACTION_START_RECOGNITION = "org.vosk.demo.ACTION_START";
    public static final String ACTION_STOP_RECOGNITION = "org.vosk.demo.ACTION_STOP";

    private Model model;
    private SpeechService speechService;
    private PowerManager.WakeLock wakeLock;
    private boolean isRunning = false;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
        createNotificationChannel();
        acquireWakeLock();
        startForeground(NOTIFICATION_ID, createNotification());
        initModel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_START_RECOGNITION:
                    startListening();
                    break;
                case ACTION_STOP_RECOGNITION:
                    stopListening();
                    break;
            }
        }
        return START_STICKY;
    }



    private void initModel() {
        if (model != null) {
            Log.d(TAG, "Model already loaded");
            startListening(); // <-- safe to start here
            return;
        }

        StorageService.unpack(this, "model-en-us", "model",
                (model) -> {
                    this.model = model;
                    Log.d(TAG, "Model loaded successfully");
                    verifyModel();
                    startListening(); // <-- now model is ready, safe to start
                },
                (exception) -> {
                    Log.e(TAG, "Model init failed: " + exception.getMessage());
                    stopSelf();
                });
    }


    private void startListening() {
        if (isRunning || model == null) {
            Log.w(TAG, "startListening() skipped: model not ready or already running");
            return;
        }

        try {
            Recognizer recognizer = new Recognizer(model, 16000.0f);
            speechService = new SpeechService(recognizer, 16000.0f);
            speechService.startListening(this);
            isRunning = true;
            updateNotification();
            Log.d(TAG, "Speech recognition started");
        } catch (Exception e) {
            Log.e(TAG, "Error starting recognition", e);
        }
    }

    private void stopListening() {
        if (!isRunning) return;

        if (speechService != null) {
            speechService.stop();
            speechService.shutdown();
            speechService = null;
        }

        isRunning = false;
        updateNotification();
        Log.d(TAG, "Speech recognition stopped");
    }

    private void verifyModel() {
        File modelDir = new File(getExternalFilesDir(null), "model/model-en-us");
        if (!modelDir.exists()) {
            Log.e(TAG, "Model directory not found");
            stopSelf();
        }
    }

    private void updateNotification() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.notify(NOTIFICATION_ID, createNotification());
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Voice Recognition")
                .setContentText(isRunning ? "Listening..." : "Idle")
                .setSmallIcon(R.drawable.ic_mic)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Vosk Voice Recognition",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Runs continuous speech recognition in background");
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private void acquireWakeLock() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VoskService::WakeLock");
        wakeLock.acquire();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopListening();
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        Log.d(TAG, "Service destroyed");
    }

    @Override
    public void onResult(String hypothesis) {
        try {
            JSONObject json = new JSONObject(hypothesis);
            String text = json.getString("text");
            Log.d(TAG, "Detected: " + text);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse result", e);
        }
    }

    @Override
    public void onFinalResult(String hypothesis) { }

    @Override
    public void onPartialResult(String hypothesis) { }

    @Override
    public void onError(Exception e) {
        Log.e(TAG, "Recognition error", e);
        stopListening();
    }

    @Override
    public void onTimeout() {
        Log.d(TAG, "Recognition timeout");
        stopListening();
    }
}
