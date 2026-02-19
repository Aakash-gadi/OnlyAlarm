package com.android.randoalarm;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.android.randoalarm.R;

public class AlarmRingingActivity extends AppCompatActivity {

    private static final String TAG = "AlarmRingingActivity";
    Button btnStop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // CRITICAL: Set flags BEFORE setContentView for newer Android
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);

            // Also set window flags as backup
            getWindow().addFlags(
                    android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                            android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                            android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

            android.app.KeyguardManager keyguardManager = (android.app.KeyguardManager) getSystemService(
                    Context.KEYGUARD_SERVICE);
            if (keyguardManager != null) {
                keyguardManager.requestDismissKeyguard(this, null);
            }
        } else {
            // For older devices
            getWindow().addFlags(
                    android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                            android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                            android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                            android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }

        setContentView(R.layout.activity_alarm_ringing);

        btnStop = findViewById(R.id.btnStop);

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopAlarm();
            }
        });
    }

    private void stopAlarm() {
        // Stop the service
        Intent serviceIntent = new Intent(this, AlarmService.class);
        stopService(serviceIntent);

        // Also call stopAlarm on the service instance if available
        AlarmService service = AlarmService.getInstance();
        if (service != null) {
            service.stopAlarm();
        }

        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove handler callbacks to prevent memory leaks

    }

    @Override
    public void onBackPressed() {
        // Prevent back button from closing alarm
        // User must press Stop button
    }
}