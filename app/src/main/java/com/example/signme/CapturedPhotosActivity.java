package com.example.signme;

import android.content.Intent;
import android.os.Bundle;
import android.widget.GridView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CapturedPhotosActivity extends AppCompatActivity {

    private GridView photosGrid;
    private ConnectionClass connectionClass;
    private ExecutorService executorService;
    private String email;
    private String signName;
    private List<String> capturedFrameUrls;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_captured_photos);

        // Initialize views
        photosGrid = findViewById(R.id.photosGrid);
        connectionClass = new ConnectionClass();
        executorService = Executors.newSingleThreadExecutor();
        capturedFrameUrls = new ArrayList<>();

        // Receive data from intent
        if (getIntent() != null) {
            email = getIntent().getStringExtra("email");
            signName = getIntent().getStringExtra("signName");
        } else {
            Toast.makeText(this, "Data not found", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Fetch and display captured photos
        fetchCapturedPhotos();
    }

    private void fetchCapturedPhotos() {
        executorService.execute(() -> {
            try {
                Connection con = connectionClass.CONN();
                if (con == null) {
                    runOnUiThread(() -> Toast.makeText(this, "Error in connection with MySQL server", Toast.LENGTH_SHORT).show());
                    return;
                }

                String query = "SELECT CAPTURED_FRAME FROM record_video WHERE EMAIL = ? AND SIGN_NAME = ?";
                PreparedStatement stmt = con.prepareStatement(query);
                stmt.setString(1, email);
                stmt.setString(2, signName);

                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    String capturedFrameUrl = rs.getString("CAPTURED_FRAME");
                    capturedFrameUrls.add(capturedFrameUrl);
                }

                rs.close();
                stmt.close();
                con.close();

                // Update UI on the main thread
                runOnUiThread(() -> {
                    // Initialize GridView adapter
                    PhotosGridAdapter adapter = new PhotosGridAdapter(capturedFrameUrls);
                    photosGrid.setAdapter(adapter);

                    // Handle item click to view image
                    photosGrid.setOnItemClickListener((parent, view, position, id) -> {
                        String imageUrl = capturedFrameUrls.get(position);
                        Intent intent = new Intent(CapturedPhotosActivity.this, ViewImageActivity.class);
                        intent.putExtra("imageUrl", imageUrl);
                        startActivity(intent);
                    });
                });

            } catch (SQLException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "SQL Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } finally {
                executorService.shutdown();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Shutdown executorService when activity is destroyed
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
