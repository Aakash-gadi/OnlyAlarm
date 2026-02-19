package com.android.randoalarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;

public class AlarmService extends Service {
    private static final String TAG = "AlarmService";
    private static final String CHANNEL_ID = "AlarmChannel";
    private static final int NOTIFICATION_ID = 1;
    public static final String ACTION_STOP = "STOP_ALARM";

    private MediaPlayer mediaPlayer;
    private ArrayList<MusicModel> musicList;
    private ArrayList<MusicModel> shuffledList;
    private int currentIndex = 0;
    private PowerManager.WakeLock wakeLock;

    // Static instance to communicate with activity
    private static AlarmService instance;
    private String currentSongName = "Alarm Ringing";

    public static AlarmService getInstance() {
        return instance;
    }

    public String getCurrentSongName() {
        return currentSongName;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Log.d(TAG, "Service created");

        // Acquire wake lock to keep CPU running
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AlarmApp::AlarmWakeLock");
        wakeLock.acquire(30 * 60 * 1000L); // 30 minutes max

        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");

        // Check if this is a stop command
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopAlarm();
            return START_NOT_STICKY;
        }

        // Start as foreground service with full screen intent
        startForeground(NOTIFICATION_ID, createNotification(currentSongName));

        // Load and play music
        loadMusicList();

        if (musicList != null && !musicList.isEmpty()) {
            // Create shuffled list
            shuffledList = new ArrayList<>(musicList);
            Collections.shuffle(shuffledList);
            currentIndex = 0;
            Log.d(TAG, "Shuffled playlist with " + shuffledList.size() + " songs");
            playCurrentSong();
        } else {
            Log.e(TAG, "No music available");
            currentSongName = "No music found";
            updateNotification(currentSongName);
        }

        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Alarm Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Alarm is ringing");
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.enableVibration(true);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification(String songName) {
        // Create intent for the activity
        Intent activityIntent = new Intent(this, AlarmRingingActivity.class);
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent activityPendingIntent = PendingIntent.getActivity(
                this,
                0,
                activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Create stop intent
        Intent stopIntent = new Intent(this, AlarmService.class);
        stopIntent.setAction(ACTION_STOP);

        PendingIntent stopPendingIntent = PendingIntent.getService(
                this,
                1,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Alarm Ringing")
                .setContentText(songName)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(activityPendingIntent)
                .setFullScreenIntent(activityPendingIntent, true) // This launches full screen
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent);

        return builder.build();
    }

    private void updateNotification(String songName) {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, createNotification(songName));
        }
    }

    private void loadMusicList() {
        SharedPreferences prefs = getSharedPreferences("MusicPrefs", MODE_PRIVATE);
        String json = prefs.getString("musicList", null);

        if (json != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<MusicModel>>() {}.getType();
            musicList = gson.fromJson(json, type);
            Log.d(TAG, "Loaded " + musicList.size() + " songs");
        } else {
            musicList = new ArrayList<>();
            Log.d(TAG, "No music list found");
        }
    }

    private void playCurrentSong() {
        if (shuffledList == null || shuffledList.isEmpty()) {
            Log.e(TAG, "No songs to play");
            currentSongName = "No music available";
            updateNotification(currentSongName);
            return;
        }

        // Check if we need to reshuffle
        if (currentIndex >= shuffledList.size()) {
            Collections.shuffle(shuffledList);
            currentIndex = 0;
            Log.d(TAG, "Reshuffling playlist - all songs played");
        }

        MusicModel currentMusic = shuffledList.get(currentIndex);
        Log.d(TAG, "Playing song #" + (currentIndex + 1) + ": " + currentMusic.name);

        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
            }

            mediaPlayer = new MediaPlayer();
            Uri musicUri = Uri.parse(currentMusic.uri);
            mediaPlayer.setDataSource(this, musicUri);
            mediaPlayer.prepare();
            mediaPlayer.start();

            currentSongName = currentMusic.name;
            updateNotification("Playing: " + currentMusic.name);
            Log.d(TAG, "Successfully started playing: " + currentMusic.name);

            // When song completes, play next song
            mediaPlayer.setOnCompletionListener(mp -> {
                Log.d(TAG, "Song completed, moving to next");
                currentIndex++;
                playCurrentSong();
            });

            // Handle errors
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error: what=" + what + " extra=" + extra);
                currentIndex++;
                if (currentIndex < shuffledList.size()) {
                    playCurrentSong();
                } else {
                    currentSongName = "Playback error";
                    updateNotification(currentSongName);
                }
                return true;
            });

        } catch (Exception e) {
            Log.e(TAG, "Error playing song: " + e.getMessage());
            e.printStackTrace();

            // Try next song
            currentIndex++;
            if (currentIndex < shuffledList.size()) {
                playCurrentSong();
            } else {
                currentSongName = "Unable to play music";
                updateNotification(currentSongName);
            }
        }
    }

    public void stopAlarm() {
        Log.d(TAG, "Stopping alarm");

        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }

        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");

        instance = null;

        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }

        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}