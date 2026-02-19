package com.android.randoalarm;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarm received!");

        // Acquire FULL wake lock to turn screen on
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK |
                        PowerManager.ACQUIRE_CAUSES_WAKEUP |
                        PowerManager.ON_AFTER_RELEASE,
                "AlarmApp::AlarmWakeLock"
        );
        wakeLock.acquire(10000); // Hold for 10 seconds

        // Start the foreground service first
        Intent serviceIntent = new Intent(context, AlarmService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }

        // Launch the full-screen activity
        Intent activityIntent = new Intent(context, AlarmRingingActivity.class);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK |
                Intent.FLAG_ACTIVITY_NO_USER_ACTION);

        try {
            context.startActivity(activityIntent);
            Log.d(TAG, "Activity launched successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch activity: " + e.getMessage());
            e.printStackTrace();
        }

        // Release wake lock after a delay
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (wakeLock.isHeld()) {
                    wakeLock.release();
                }
            }
        }, 10000);
    }
}