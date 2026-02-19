package com.android.randoalarm;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.lang.reflect.Type;
import java.util.ArrayList;

import com.android.randoalarm.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.Calendar;
//import com.android.randoalarm.R;

public class MainActivity extends AppCompatActivity {
    Button b;
    ImageView music;
    ListView listView;
    ArrayList<AlarmModel> alarmList;
    AlarmAdapter adapter;

    private static final int PERMISSION_REQUEST_CODE = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Request permissions
        requestPermissions();

        listView = findViewById(R.id.listView);

        alarmList = loadAlarms();
        adapter = new AlarmAdapter(this, alarmList);
        listView.setAdapter(adapter);

        b = findViewById(R.id.button);
        music = findViewById(R.id.music);

        Intent intent = new Intent(this, add.class);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(intent, 100);
            }
        });

        Intent intent2 = new Intent(this, musicbutton.class);
        music.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(intent2);
            }
        });
    }

    private void requestPermissions() {
        // For Android 13+ (API 33+), request notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[] { Manifest.permission.POST_NOTIFICATIONS },
                        PERMISSION_REQUEST_CODE);
            }
        }

        // For Android 12+ (API 31+), check exact alarm permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(this,
                        "Please enable exact alarm permission for this app",
                        Toast.LENGTH_LONG).show();

                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            int hour = data.getIntExtra("hour", 0);
            int minute = data.getIntExtra("minute", 0);
            String amPm = data.getStringExtra("ampm");
            String days = data.getStringExtra("days");

            // Format the time string properly
            String formattedTime = String.format("%02d:%02d %s", hour, minute, amPm);

            // Create new AlarmModel
            AlarmModel newAlarm = new AlarmModel(formattedTime, days, true);

            // Convert to 24-hour format for AlarmManager
            int hour24 = hour;
            if (amPm.equals("PM") && hour != 12) {
                hour24 = hour + 12;
            } else if (amPm.equals("AM") && hour == 12) {
                hour24 = 0;
            }

            newAlarm.setHourMinute(hour24, minute);
            alarmList.add(newAlarm);

            // Schedule the alarm
            scheduleAlarm(newAlarm);

            // Notify adapter
            adapter.notifyDataSetChanged();

            // Save
            saveAlarms();

            Toast.makeText(this, "Alarm added: " + formattedTime, Toast.LENGTH_SHORT).show();
        }
    }

    public void scheduleAlarm(AlarmModel alarm) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                alarm.alarmId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, alarm.hour);
        calendar.set(Calendar.MINUTE, alarm.minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // If time has passed today, schedule for tomorrow
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        if (alarmManager != null) {
            try {
                // Use setAlarmClock for most reliable alarms
                AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(
                        calendar.getTimeInMillis(),
                        pendingIntent);
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent);

                Log.d("MainActivity", "Alarm scheduled for: " + calendar.getTime());
                Toast.makeText(this, "Alarm set for: " + calendar.getTime(), Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e("MainActivity", "Error scheduling alarm: " + e.getMessage());
                Toast.makeText(this, "Error setting alarm", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void cancelAlarm(AlarmModel alarm) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                alarm.alarmId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            Toast.makeText(this, "Alarm cancelled", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveAlarms() {
        SharedPreferences sp = getSharedPreferences("alarms", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        Gson gson = new Gson();
        editor.putString("alarm_list", gson.toJson(alarmList));
        editor.apply();
    }

    private ArrayList<AlarmModel> loadAlarms() {
        SharedPreferences sp = getSharedPreferences("alarms", MODE_PRIVATE);
        String json = sp.getString("alarm_list", null);

        if (json == null)
            return new ArrayList<>();

        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<AlarmModel>>() {
        }.getType();
        return gson.fromJson(json, type);
    }
}