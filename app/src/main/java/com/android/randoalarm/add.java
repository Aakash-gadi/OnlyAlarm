package com.android.randoalarm;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.randoalarm.R;

public class add extends AppCompatActivity {

    public static final String hour = "com.android.onlyalarm.hour";

    TimePicker timePicker;
    TextView wrong;
    TextView right;
    public int formattedHour;
    public int formattedMinute;
    public String amPm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            wrong = findViewById(R.id.wrong);
            right = findViewById(R.id.right);
            timePicker = findViewById(R.id.timePicker);
            timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
                @Override
                public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                    int hour = hourOfDay;

                    // Determine AM or PM and adjust hour
                    if (hour == 0) {
                        hour += 12;
                        amPm = "AM";
                    } else if (hour == 12) {
                        amPm = "PM";
                    } else if (hour > 12) {
                        hour -= 12;
                        amPm = "PM";
                    } else {
                        amPm = "AM";
                    }

                    formattedHour = hour;
                    formattedMinute = minute;

                    // Format hour and minute for display
                    // formattedHour = (hour < 10) ? "0" + hour : String.valueOf(hour);
                    // formattedMinute = (minute < 10) ? "0" + minute : String.valueOf(minute);

                }
            });
            wrong.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finish();

                }
            });
            right.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Get time directly from picker
                    int hour = timePicker.getHour();
                    int minute = timePicker.getMinute();
                    String currentAmPm;

                    // Calculate AM/PM and 12-hour format
                    if (hour == 0) {
                        hour = 12;
                        currentAmPm = "AM";
                    } else if (hour == 12) {
                        currentAmPm = "PM";
                    } else if (hour > 12) {
                        hour -= 12;
                        currentAmPm = "PM";
                    } else {
                        currentAmPm = "AM";
                    }

                    Toast.makeText(add.this, "Alarm set successfully", Toast.LENGTH_SHORT).show();
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("hour", hour);
                    resultIntent.putExtra("minute", minute);
                    resultIntent.putExtra("ampm", currentAmPm);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                }
            });
            return insets;
        });
    }
}