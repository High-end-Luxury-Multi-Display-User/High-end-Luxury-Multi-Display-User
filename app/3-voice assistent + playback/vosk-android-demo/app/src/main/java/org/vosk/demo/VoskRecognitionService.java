package org.vosk.demo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
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
            startListening();
            return;
        }

        StorageService.unpack(this, "model-en-us", "model",
                (model) -> {
                    this.model = model;
                    startListening();
                },
                (exception) -> {
                    Log.e(TAG, "Model unpack failed", exception);
                    stopSelf();
                });
    }

    private void startListening() {
        if (isRunning || model == null) return;

        try {
            Recognizer recognizer = new Recognizer(model, 16000.0f);
            speechService = new SpeechService(recognizer, 16000.0f);
            speechService.startListening(this);
            isRunning = true;
            updateNotification();
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
        stopListening();
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        super.onDestroy();
    }

    @Override
    public void onResult(String hypothesis) {
        try {
            JSONObject json = new JSONObject(hypothesis);
            String text = json.getString("text").toLowerCase().trim();
            text = text.replaceAll("\\s+", " ");
            Log.d(TAG, "Recognized: " + text);

            String command = null;

            String[] greetSynonyms = {"hi", "hey", "high", "hello", "hail"};
            String[] carSynonyms = {"car", "call", "court", "assistant", "vehicle", "system", "core"};
            String[] lightSynonyms = {"light", "lite", "like", "lamp", "bulb", "glow"};
            String[] modeSynonyms = {"mode", "mood", "move", "moved", "setting", "style" , "moon"};
            String[] maxSynonyms = {"maximum", "max", "full"};
            String[] minSynonyms = {"minimum", "min", "meme", "low", "small"};
            String[] daySynonyms = {"day", "they", "the"};
            String[] nightSynonyms = {"night", "nine"};
            String[] seatSynonyms = {"seat", "seek", "see" , "the"};
            String[] offSynonyms = {"off", "of", "oh"};

            String greetPattern = String.join("|", greetSynonyms);
            String carPattern = String.join("|", carSynonyms);
            String lightPattern = String.join("|", lightSynonyms);
            String modePattern = String.join("|", modeSynonyms);
            String maxPattern = String.join("|", maxSynonyms);
            String minPattern = String.join("|", minSynonyms);
            String dayPattern = String.join("|", daySynonyms);
            String nightPattern = String.join("|", nightSynonyms);
            String seatPattern = String.join("|", seatSynonyms);
            String offPattern = String.join("|", offSynonyms);

            if (text.matches(".*\\b(" + greetPattern + ")\\s+(" + carPattern + ")\\b.*")) {
                command = "greet";
            }else if (text.matches(".*\\b(" + seatPattern + ")\\s+default\\b.*")) {
                command = "seat_70";
            } else if (text.matches(".*\\b(" + seatPattern + ")\\s+(" + maxPattern + ")\\b.*")) {
                command = "seat_90";
            }else if (text.matches(".*\\b(" + seatPattern + ")\\s+(" + minPattern + ")\\b.*")) {
                command = "seat_45";
            } else if (text.matches(".*\\b(" + lightPattern + ")\\s+on\\b.*")) {
                command = "light_on";
            } else if (text.matches(".*\\b(" + lightPattern + ")\\s+(" + offPattern + ")\\b.*")) {
                command = "light_off";
            } else if (text.matches(".*\\b(" + dayPattern + ")\\s+(" + modePattern + ")\\b.*")) {
                command = "day_mode";
            } else if (text.matches(".*\\b(" + nightPattern + ")\\s+(" + modePattern + ")\\b.*")) {
                command = "night_mode";
            }

            if (command != null) {
                Intent intent = new Intent("org.vosk.demo.ACTION_COMMAND");
                intent.putExtra("command", command);
                sendBroadcast(intent);
            } else {
                Log.d(TAG, "Unrecognized command: " + text);
            }

        } catch (JSONException e) {
            Log.e(TAG, "JSON parsing error", e);
        }
    }


    @Override public void onFinalResult(String hypothesis) {}
    @Override public void onPartialResult(String hypothesis) {}
    @Override public void onError(Exception e) { stopListening(); }
    @Override public void onTimeout() { stopListening(); }
}
