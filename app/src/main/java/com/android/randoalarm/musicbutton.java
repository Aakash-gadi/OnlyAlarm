package com.android.randoalarm;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.randoalarm.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class musicbutton extends AppCompatActivity {

    Button btnAddMusic;

    RecyclerView recyclerView;
    MusicAdapter adapter;
    ArrayList<MusicModel> musicList = new ArrayList<>();

    ActivityResultLauncher<Intent> musicPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();

                    if (uri != null) {
                        try {
                            // Take persistable permission
                            getContentResolver().takePersistableUriPermission(
                                    uri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION);

                            String name = getFileName(uri);
                            musicList.add(new MusicModel(name, uri.toString()));
                            adapter.notifyDataSetChanged();
                            saveMusicList();

                            Toast.makeText(this, "Added: " + name, Toast.LENGTH_SHORT).show();

                        } catch (SecurityException e) {
                            Toast.makeText(this, "Permission error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            Log.e("MusicButton", "Permission error", e);
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);

        btnAddMusic = findViewById(R.id.btnAddMusic);

        recyclerView = findViewById(R.id.recyclerMusic);

        // Load saved music list
        loadMusicList();

        adapter = new MusicAdapter(musicList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        btnAddMusic.setOnClickListener(v -> openMusicPicker());

        // Test button

    }

    private void openMusicPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("audio/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        musicPickerLauncher.launch(intent);
    }

    private void testPlayMusic(MusicModel music) {
        try {
            MediaPlayer testPlayer = new MediaPlayer();
            Uri musicUri = Uri.parse(music.uri);

            testPlayer.setDataSource(this, musicUri);
            testPlayer.prepare();
            testPlayer.start();

            Toast.makeText(this, "Playing: " + music.name, Toast.LENGTH_SHORT).show();
            Log.d("MusicTest", "Successfully playing: " + music.name);

            testPlayer.setOnCompletionListener(mp -> {
                mp.release();
                Toast.makeText(this, "Test complete", Toast.LENGTH_SHORT).show();
            });

        } catch (Exception e) {
            Toast.makeText(this, "Test failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("MusicTest", "Error playing music", e);
        }
    }

    private String getFileName(Uri uri) {
        String result = "Unknown";
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                Log.e("MusicButton", "Error getting filename", e);
            }
        }
        return result;
    }

    private void saveMusicList() {
        SharedPreferences prefs = getSharedPreferences("MusicPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        Gson gson = new Gson();
        String json = gson.toJson(musicList);
        editor.putString("musicList", json);
        editor.apply();

        Log.d("MusicButton", "Saved " + musicList.size() + " songs");
    }

    private void loadMusicList() {
        SharedPreferences prefs = getSharedPreferences("MusicPrefs", MODE_PRIVATE);
        String json = prefs.getString("musicList", null);

        if (json != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<MusicModel>>() {
            }.getType();
            musicList = gson.fromJson(json, type);
            Log.d("MusicButton", "Loaded " + musicList.size() + " songs");
        } else {
            musicList = new ArrayList<>();
        }
    }
}