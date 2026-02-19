package com.android.randoalarm;

public class AlarmModel {
    String time;
    String days;
    boolean isEnabled;
    int alarmId; // Add this to uniquely identify each alarm
    int hour;    // Store raw hour for scheduling
    int minute;  // Store raw minute for scheduling

    public AlarmModel(String time, String days, boolean isEnabled) {
        this.time = time;
        this.days = days;
        this.isEnabled = isEnabled;
        this.alarmId = (int) System.currentTimeMillis(); // Unique ID
    }

    // Add setters for hour and minute
    public void setHourMinute(int hour, int minute) {
        this.hour = hour;
        this.minute = minute;
    }
}