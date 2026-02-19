package com.android.randoalarm;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Switch;
import android.widget.TextView;

import com.android.randoalarm.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

import java.util.ArrayList;

public class AlarmAdapter extends BaseAdapter {

    Context context;
    ArrayList<AlarmModel> alarmList;

    public AlarmAdapter(Context context, ArrayList<AlarmModel> alarmList) {
        this.context = context;
        this.alarmList = alarmList;
    }

    @Override
    public int getCount() {
        return alarmList.size();
    }

    @Override
    public Object getItem(int position) {
        return alarmList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.listviewtemplate, parent, false);
        }

        TextView tvTime = convertView.findViewById(R.id.tvTime);
        TextView tvDays = convertView.findViewById(R.id.tvDays);
        Switch switchAlarm = convertView.findViewById(R.id.switchAlarm);

        AlarmModel model = alarmList.get(position);

        tvTime.setText(model.time);
        tvDays.setText(model.days);
        switchAlarm.setChecked(model.isEnabled);

        switchAlarm.setOnCheckedChangeListener((buttonView, isChecked) -> {
            model.isEnabled = isChecked;
            saveAlarms(); // save change

            if (context instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) context;
                if (isChecked) {
                    mainActivity.scheduleAlarm(model);
                } else {
                    mainActivity.cancelAlarm(model);
                }
            }
        });

        return convertView;
    }

    // 🔐 Permanent storage
    private void saveAlarms() {
        SharedPreferences sp = context.getSharedPreferences("alarms", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        Gson gson = new Gson();
        editor.putString("alarm_list", gson.toJson(alarmList));
        editor.apply();
    }
}
