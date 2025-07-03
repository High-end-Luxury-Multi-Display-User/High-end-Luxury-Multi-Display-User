package org.vosk.demo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.widget.ImageView;
import android.widget.Toast;

public class VoiceCommandReceiver extends BroadcastReceiver {

    private final ImageView statusImage;
    private final Context context;
    private String lastCommand = "";

    public VoiceCommandReceiver(Context context, ImageView statusImage) {
        this.context = context;
        this.statusImage = statusImage;
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        String command = intent.getStringExtra("command");
        if (command == null || command.equals(lastCommand)) return;

        lastCommand = command;

        switch (command) {
            case "greet":
                Toast.makeText(context, "Hello! How can I assist you?", Toast.LENGTH_SHORT).show();
                playAudio(R.raw.greet);
                break;
            case "light_on":
                statusImage.setImageResource(R.drawable.ledon);
                playAudio(R.raw.lighton);
                break;
            case "light_off":
                statusImage.setImageResource(R.drawable.ledoff);
                playAudio(R.raw.lightoff);
                break;
            case "day_mode":
                statusImage.setImageResource(R.drawable.sun);
                playAudio(R.raw.day);
                break;
            case "night_mode":
                statusImage.setImageResource(R.drawable.moon);
                playAudio(R.raw.night);
                break;
            case "seat_45":
                statusImage.setImageResource(R.drawable.seat_45);
                playAudio(R.raw.seat);
                break;
            case "seat_70":
                statusImage.setImageResource(R.drawable.seat_70);
                playAudio(R.raw.seatdefault);
                break;
            case "seat_90":
                statusImage.setImageResource(R.drawable.seat_90);
                playAudio(R.raw.seatmax);
                break;
        }
    }

    private void playAudio(int resId) {
        MediaPlayer player = MediaPlayer.create(context, resId);
        player.setOnCompletionListener(MediaPlayer::release);
        player.start();
    }
}
