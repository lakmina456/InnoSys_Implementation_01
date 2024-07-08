package com.example.signme;

import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SessionActivity extends AppCompatActivity {

    private ConnectionClass connectionClass;
    private Connection con;
    private ImageView imageView, imageView1, signView;
    private TextView labelView;
    private Handler handler = new Handler();
    private Timer timer = new Timer();
    private MediaPlayer mediaPlayer;
    private List<String> audioUrls;
    String str;
    private static final String TAG = "SessionActivity";
    private Runnable runnable;
    private boolean isMuted = false;
    private TextView dateTimeView;
    private ImageButton muteUnmuteButton;
    private ImageButton endSessionButton;
    private AudioManager audioManager;
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session);

        imageView = findViewById(R.id.imgGlide);
        imageView1 = findViewById(R.id.imgGlide1);
        labelView = findViewById(R.id.label_view);
        signView = findViewById(R.id.sign_view);
        dateTimeView = findViewById(R.id.date_time_view);
        muteUnmuteButton = findViewById(R.id.mute_unmute);
        endSessionButton = findViewById(R.id.end_session_button);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        // Retrieve email from intent
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("email")) {
            email = intent.getStringExtra("email");
        }

        // Initialize ConnectionClass
        connectionClass = new ConnectionClass();
        mediaPlayer = new MediaPlayer();
        audioUrls = new ArrayList<>();

        connect();
        imgGlide();
        updateLabelAndSign();
        fetchAudioUrls();

        // Initialize handler for periodic updates
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                updateDateTime();
                handler.postDelayed(this, 2000);
            }
        };
        handler.post(runnable);

        // Setup mute/unmute button click listener
        muteUnmuteButton.setOnClickListener(v -> toggleMute());

        // Setup end session button click listener
        endSessionButton.setOnClickListener(v -> showEndSessionDialog());
    }



    private void endSession() {
        String sessionEndTime = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date());

        saveSessionEndTime(email, sessionEndTime);

        // Navigate back to DriveActivity
        Intent intent = new Intent(SessionActivity.this, DriveActivity.class);
        intent.putExtra("email", email);
        startActivity(intent);
        finish();
    }

    private void saveSessionEndTime(String email, String sessionEndTime) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            try {
                con = connectionClass.CONN();
                if (con == null) {
                    runOnUiThread(() -> Toast.makeText(SessionActivity.this, "Error in connection with MySQL server", Toast.LENGTH_SHORT).show());
                    return;
                }

                String query = "UPDATE session SET SESSION_END_TIME = ? WHERE EMAIL = ? AND SESSION_END_TIME IS NULL";
                PreparedStatement stmt = con.prepareStatement(query);
                stmt.setString(1, sessionEndTime);
                stmt.setString(2, email);

                int rowsUpdated = stmt.executeUpdate();
                runOnUiThread(() -> {
                    if (rowsUpdated > 0) {
                        Toast.makeText(SessionActivity.this, "Session ended and saved successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(SessionActivity.this, "Failed to end session", Toast.LENGTH_SHORT).show();
                    }
                });

                stmt.close();
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(SessionActivity.this, "SQL Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(SessionActivity.this, "Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showEndSessionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("End Session")
                .setMessage("Are you sure you want to end the session?")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        endSession(); // Call endSession to save the end time and navigate back
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    private void updateDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String currentDateAndTime = sdf.format(new Date ());
        dateTimeView.setText(currentDateAndTime);
    }

    private void toggleMute() {
        if (isMuted) {
            audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
            muteUnmuteButton.setImageResource(R.drawable.volume);
            Toast.makeText(SessionActivity.this, "Sound is unmuted", Toast.LENGTH_SHORT).show();
        } else {
            audioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
            muteUnmuteButton.setImageResource(R.drawable.volume_off);
            Toast.makeText(SessionActivity.this, "Sound is muted", Toast.LENGTH_SHORT).show();
        }
        isMuted = !isMuted;
    }

    public void imgGlide() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            PreparedStatement stmt = null;
            ResultSet rs = null;
            List<String> urlList = new ArrayList<>();
            try {
                con = connectionClass.CONN();
                if (con == null) {
                    Log.e(TAG, "Database connection failed");
                    return;
                }
                String query = "SELECT CAPTURED_FRAME FROM record_video WHERE VIDEO_ID = 'v001'";
                stmt = con.prepareStatement(query);
                rs = stmt.executeQuery();

                while (rs.next()) {
                    urlList.add(rs.getString("CAPTURED_FRAME").trim());
                }
            } catch (SQLException e) {
                Log.e(TAG, "SQL Exception: ", e);
            } finally {
                try {
                    if (rs != null) rs.close();
                    if (stmt != null) stmt.close();
                    if (con != null) con.close();
                } catch (SQLException e) {
                    Log.e(TAG, "Error closing resources: ", e);
                }
            }

            runOnUiThread(() -> {
                if (urlList.isEmpty()) {
                    Toast.makeText(SessionActivity.this, "No URLs found", Toast.LENGTH_SHORT).show();
                } else {
                    displayImages(urlList);
                }
            });
        });
    }

    private void displayImages(List<String> urlList) {
        final Handler handler = new Handler();
        final int[] index = {0};
        final Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (!urlList.isEmpty()) {
                    String url = urlList.get(index[0]);
                    Log.d(TAG, "Image URL/Path: " + url);

                    if (url.startsWith("http") || url.startsWith("https")) {
                        if (index[0] % 2 == 0) {
                            imageView.startAnimation(fadeIn);
                            Glide.with(SessionActivity.this).load(url).into(imageView);
                        } else {
                            imageView1.startAnimation(fadeIn);
                            Glide.with(SessionActivity.this).load(url).into(imageView1);
                        }
                    } else {
                        Toast.makeText(SessionActivity.this, "Invalid URL or Path: " + url, Toast.LENGTH_SHORT).show();
                    }
                    index[0]++;
                    if (index[0] >= urlList.size()) {
                        index[0] = 0; // Reset index to loop the images
                    }
                    handler.postDelayed(this, 2000); // Change image every second
                }
            }
        };
        handler.post(runnable);
    }

    public void connect() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            try {
                con = connectionClass.CONN();
                if (con == null) {
                    str = "Error in connection with MySQL Server";
                } else {
                    str = "Connected with MySQL server";
                }
            } catch (Exception e) {
                Log.e(TAG, "Connection Exception: ", e);
                str = "Exception in connection";
            } finally {
                runOnUiThread(() -> Toast.makeText(SessionActivity.this, str, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void updateLabelAndSign() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                con = connectionClass.CONN();
                if (con == null) {
                    Log.e(TAG, "Database connection failed");
                    return;
                }
                String query = "SELECT SIGN_NAME, SEGMENTED_SIGN FROM record_video WHERE VIDEO_ID = 'v001'";
                stmt = con.prepareStatement(query);
                rs = stmt.executeQuery();

                List<String> labels = new ArrayList<>();
                List<String> signUrls = new ArrayList<>();

                while (rs.next()) {
                    labels.add(rs.getString("SIGN_NAME"));
                    signUrls.add(rs.getString("SEGMENTED_SIGN"));
                }

                runOnUiThread(() -> {
                    updateLabelView(labels);
                    updateSignView(signUrls);
                });
            } catch (SQLException e) {
                Log.e(TAG, "SQL Exception: ", e);
            } finally {
                try {
                    if (rs != null) rs.close();
                    if (stmt != null) stmt.close();
                    if (con != null) con.close();
                } catch (SQLException e) {
                    Log.e(TAG, "Error closing resources: ", e);
                }
            }
        });
    }

    private void updateLabelView(List<String> labels) {
        final Handler handler = new Handler();
        final int[] index = {0};

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (!labels.isEmpty()) {
                    String label = labels.get(index[0]);
                    labelView.setText(label);
                    index[0]++;
                    if (index[0] >= labels.size()) {
                        index[0] = 0;
                    }
                    handler.postDelayed(this, 2000);
                }
            }
        };
        handler.post(runnable);
    }

    private void updateSignView(List<String> signUrls) {
        final Handler handler = new Handler();
        final int[] index = {0};
        final Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (!signUrls.isEmpty()) {
                    String signUrl = signUrls.get(index[0]);
                    if (signUrl != null && !signUrl.isEmpty()) {
                        signView.startAnimation(fadeIn);
                        Glide.with(SessionActivity.this).load(signUrl).into(signView);
                    }
                    index[0]++;
                    if (index[0] >= signUrls.size()) {
                        index[0] = 0;
                    }
                    handler.postDelayed(this, 2000);
                }
            }
        };
        handler.post(runnable);
    }

    public void fetchAudioUrls() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                con = connectionClass.CONN();
                if (con == null) {
                    Log.e(TAG, "Database connection failed");
                    return;
                }
                String query = "SELECT AUDIO_ALERT FROM record_video WHERE VIDEO_ID = 'v001'";
                stmt = con.prepareStatement(query);
                rs = stmt.executeQuery();

                audioUrls.clear();
                while (rs.next()) {
                    String audioUrl = rs.getString("AUDIO_ALERT");
                    audioUrls.add(audioUrl);
                }

                runOnUiThread(this::playAudioSequentially);
            } catch (SQLException e) {
                Log.e(TAG, "SQL Exception: ", e);
            } finally {
                try {
                    if (rs != null) rs.close();
                    if (stmt != null) stmt.close();
                    if (con != null) con.close();
                } catch (SQLException e) {
                    Log.e(TAG, "Error closing resources: ", e);
                }
            }
        });
    }

    public void playAudioSequentially() {
        playAudioSequentially(0);
    }

    private void playAudioSequentially(int index) {
        if (index >= audioUrls.size()) {
            Toast.makeText(this, "Finished playing all audio files", Toast.LENGTH_SHORT).show();
            return;
        }

        String audioUrl = audioUrls.get(index);

        if (audioUrl == null || audioUrl.isEmpty()) {
            handler.postDelayed(() -> playAudioSequentially(index + 1), 1500); // 1-second delay for null values
        } else {
            try {
                mediaPlayer.reset();
                mediaPlayer.setDataSource(audioUrl);
                mediaPlayer.setOnPreparedListener(mp -> mediaPlayer.start());
                mediaPlayer.setOnCompletionListener(mp -> playAudioSequentially(index + 1));

                mediaPlayer.prepareAsync();
            } catch (IOException e) {
                Log.e(TAG, "Error setting data source: ", e);
                Toast.makeText(this, "Error setting data source: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                playAudioSequentially(index + 1); // Proceed to next audio even if there's an error
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Release MediaPlayer resources when activity is destroyed to avoid memory leaks
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        // Remove scheduled callbacks from handler
        handler.removeCallbacksAndMessages(null);
    }



    @Override
    protected void onPause() {
        super.onPause();
        mediaPlayer.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mediaPlayer.start();
    }


}