package com.example.signme;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.squareup.picasso.Picasso;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AnalyticsActivity extends AppCompatActivity {

    private LinearLayout analyticsContainer;
    private ConnectionClass connectionClass;
    private ExecutorService executorService;
    private ImageView profileImage;
    private TextView profileName;
    private String email;
    private String sessionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics);

        // Initialize views
        TextView headingText = findViewById(R.id.headingText);
        analyticsContainer = findViewById(R.id.analyticsContainer);
        profileImage = findViewById(R.id.profileImage);
        profileName = findViewById(R.id.profileName);
        connectionClass = new ConnectionClass();
        executorService = Executors.newSingleThreadExecutor();

        // Set heading text
        headingText.setText("Analytics");

        // Receive the email and sessionId from the previous activity
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("email") && intent.hasExtra("sessionId")) {
            email = intent.getStringExtra("email");
            sessionId = intent.getStringExtra("sessionId");
            loadProfileData(email);
            // Fetch analytics data relevant to email and sessionId
            fetchAnalyticsData(email, sessionId);
        } else {
            showToast("Email or sessions not found");
        }

        // Set up the View Graph button
        Button viewGraphButton = findViewById(R.id.graphButton);
        viewGraphButton.setOnClickListener(v -> {
            Intent graphIntent = new Intent(AnalyticsActivity.this, GraphActivity.class);
            graphIntent.putExtra("email", email);
            graphIntent.putExtra("sessionId", sessionId);
            startActivity(graphIntent);
        });
    }

    private void loadProfileData(String email) {
        executorService.execute(() -> {
            try {
                Connection con = connectionClass.CONN();
                if (con == null) {
                    runOnUiThread(() -> showToast("Error in connection with MySQL server"));
                    return;
                }

                String query = "SELECT FIRST_NAME, LAST_NAME, PROFILE_PICTURE FROM driver WHERE EMAIL = ?";
                PreparedStatement stmt = con.prepareStatement(query);
                stmt.setString(1, email);

                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    String firstName = rs.getString("FIRST_NAME");
                    String lastName = rs.getString("LAST_NAME");
                    byte[] profilePictureBytes = rs.getBytes("PROFILE_PICTURE");

                    runOnUiThread(() -> {
                        profileName.setText(firstName + "\n" + lastName);
                        if (profilePictureBytes != null) {
                            Bitmap bitmap = BitmapFactory.decodeByteArray(profilePictureBytes, 0, profilePictureBytes.length);
                            profileImage.setImageBitmap(bitmap);
                        }
                    });
                } else {
                    runOnUiThread(() -> showToast("No profile data found for this email"));
                }

                rs.close();
                stmt.close();
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
                runOnUiThread(() -> showToast("SQL Exception: " + e.getMessage()));
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> showToast("Exception: " + e.getMessage()));
            }
        });
    }

    private void fetchAnalyticsData(String email, String sessionId) {
        executorService.execute(() -> {
            Connection con = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;

            try {
                con = connectionClass.CONN();
                if (con == null) {
                    runOnUiThread(() -> showToast("Failed to connect to database"));
                    return;
                }

                // Query to fetch analytics data relevant to email and sessionId
                String query = "SELECT SEGMENTED_SIGN, SIGN_NAME " +
                        "FROM record_video " +
                        "WHERE EMAIL = ? AND SESSION_ID = ? AND SEGMENTED_SIGN IS NOT NULL AND SEGMENTED_SIGN != '' " +
                        "ORDER BY SIGN_NAME";
                stmt = con.prepareStatement(query);
                stmt.setString(1, email);
                stmt.setString(2, sessionId);

                rs = stmt.executeQuery();

                // Map to store sign names and their total counts
                Map<String, Integer> signCounts = new HashMap<>();
                Map<String, String> signUrlMap = new HashMap<>();

                String currentSignName = null;
                int currentCount = 0;
                String segmentedSignUrl = null;

                while (rs.next()) {
                    String signName = rs.getString("SIGN_NAME");
                    String currentSegmentedSignUrl = rs.getString("SEGMENTED_SIGN");

                    if (currentSignName == null) {
                        // First occurrence
                        currentSignName = signName;
                        currentCount = 1;
                        segmentedSignUrl = currentSegmentedSignUrl; // Initialize segmentedSignUrl
                    } else if (signName.equals(currentSignName)) {
                        // Same sign name as previous row
                        currentCount++;
                    } else {
                        // Different sign name, update count for previous sign name
                        if (signCounts.containsKey(currentSignName)) {
                            signCounts.put(currentSignName, signCounts.get(currentSignName) + 1);
                        } else {
                            signCounts.put(currentSignName, currentCount);
                            signUrlMap.put(currentSignName, segmentedSignUrl);
                        }
                        // Reset for the new sign name
                        currentSignName = signName;
                        currentCount = 1;
                        segmentedSignUrl = currentSegmentedSignUrl; // Update segmentedSignUrl
                    }
                }

                // Add the last sign name and count
                if (currentSignName != null) {
                    if (signCounts.containsKey(currentSignName)) {
                        signCounts.put(currentSignName, signCounts.get(currentSignName) + 1);
                    } else {
                        signCounts.put(currentSignName, currentCount);
                        signUrlMap.put(currentSignName, segmentedSignUrl);
                    }
                }

                if (signUrlMap.isEmpty()) {
                    runOnUiThread(() -> showToast("No analytics found for this session"));
                } else {
                    // Display analytics data with images
                    runOnUiThread(() -> displayAnalytics(signUrlMap, signCounts));
                }

            } catch (SQLException e) {
                e.printStackTrace();
                runOnUiThread(() -> showToast("SQL Exception: " + e.getMessage()));
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> showToast("Exception: " + e.getMessage()));
            } finally {
                try {
                    if (rs != null) rs.close();
                    if (stmt != null) stmt.close();
                    if (con != null) con.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                executorService.shutdown();
            }
        });
    }

    private void displayAnalytics(Map<String, String> signUrlMap, Map<String, Integer> signCounts) {
        // Clear the analyticsContainer before adding views
        analyticsContainer.removeAllViews();

        for (Map.Entry<String, Integer> entry : signCounts.entrySet()) {
            String signName = entry.getKey();
            int totalCount = entry.getValue();
            String segmentedSignUrl = signUrlMap.get(signName);

            // Create a view for each sign with image and total count
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.setMargins(0, 0, 0, 16);

            // Create a new linear layout for each sign
            LinearLayout signLayout = new LinearLayout(this);
            signLayout.setOrientation(LinearLayout.HORIZONTAL);
            signLayout.setLayoutParams(layoutParams);
            signLayout.setPadding(0, 0, 0, 0);
            signLayout.setBackground(getResources().getDrawable(R.drawable.rounded_rectangle));

            // Create an image view for the segmented sign
            ImageView signImageView = new ImageView(this);
            Picasso.get().load(segmentedSignUrl).into(signImageView); // Use Picasso library for image loading
            signImageView.setLayoutParams(new LinearLayout.LayoutParams(
                    150, // Adjust size as per your design
                    150));
            signImageView.setAdjustViewBounds(true);
            signLayout.addView(signImageView);

            // Create a text view for the sign name and total count
            TextView signInfoTextView = new TextView(this);
            String signInfo = signName + "\nRecorded: " + totalCount + " times";
            signInfoTextView.setText(signInfo);
            signInfoTextView.setTextColor(getResources().getColor(android.R.color.white));
            signInfoTextView.setTextSize(18);
            signInfoTextView.setTypeface(null, Typeface.BOLD);
            signInfoTextView.setPadding(20, 0, 0, 0); // Adjust padding as needed
            signLayout.addView(signInfoTextView);

            // Set click listener to open CapturedPhotosActivity
            signLayout.setOnClickListener(v -> {
                Intent intent = new Intent(AnalyticsActivity.this, CapturedPhotosActivity.class);
                intent.putExtra("email", email);
                intent.putExtra("signName", signName);
                startActivity(intent);
            });

            // Add signLayout to analyticsContainer
            analyticsContainer.addView(signLayout);
        }
    }

    private void showToast(String message) {
        // Display toast message
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
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
