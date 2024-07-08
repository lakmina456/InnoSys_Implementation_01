package com.example.signme;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HistoryActivity extends AppCompatActivity {

    private LinearLayout historyContainer;
    private ConnectionClass connectionClass;
    private ExecutorService executorService;
    private ImageView profileImage;
    private TextView profileName;
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Initialize views
        TextView headingText = findViewById(R.id.headingText);
        historyContainer = findViewById(R.id.historyContainer);
        profileImage = findViewById(R.id.profileImage);
        profileName = findViewById(R.id.profileName);
        connectionClass = new ConnectionClass();
        executorService = Executors.newSingleThreadExecutor();

        // Set heading text
        headingText.setText("History");

        // Receive the email from the previous activity
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("email")) {
            email = intent.getStringExtra("email");
            loadProfileData(email);
        } else {
            Toast.makeText(this, "Email not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Fetch session data
        fetchSessionData();
    }

    private void loadProfileData(String email) {
        executorService.execute(() -> {
            try {
                Connection con = connectionClass.CONN();
                if (con == null) {
                    runOnUiThread(() -> Toast.makeText(this, "Error in connection with MySQL server", Toast.LENGTH_SHORT).show());
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
                }

                rs.close();
                stmt.close();
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "SQL Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_history, menu);

        MenuItem menuItem = menu.findItem(R.id.menu_refresh);
        if (menuItem != null) {
            SpannableString s = new SpannableString(menuItem.getTitle());
            s.setSpan(new ForegroundColorSpan(Color.BLACK), 0, s.length(), 0);
            menuItem.setTitle(s);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_refresh) {
            refreshPage();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void refreshPage() {
        // Clear existing data
        historyContainer.removeAllViews();
        // Fetch session data again
        fetchSessionData();
    }

    private void fetchSessionData() {
        final String email = getIntent().getStringExtra("email");
        if (email == null || email.isEmpty()) {
            showToast("No email found");
            return;
        }
        final String trimmedEmail = email.trim();  // Trim any leading or trailing whitespace

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

                // Get today's and yesterday's dates
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
                Calendar calendar = Calendar.getInstance();
                String today = dateFormat.format(calendar.getTime());

                calendar.add(Calendar.DAY_OF_YEAR, -1);
                String yesterday = dateFormat.format(calendar.getTime());

                // Query to fetch session data
                String query = "SELECT SESSION_ID, START_LOCATION, DESTINATION, SESSION_START_TIME, SESSION_END_TIME " +
                        "FROM session " +
                        "WHERE EMAIL = ? " +
                        "ORDER BY SESSION_START_TIME DESC";

                stmt = con.prepareStatement(query);
                stmt.setString(1, trimmedEmail);

                rs = stmt.executeQuery();

                boolean foundSessions = false;
                while (rs.next()) {
                    foundSessions = true;
                    String sessionId = rs.getString("SESSION_ID");
                    String startLocation = rs.getString("START_LOCATION");
                    String destination = rs.getString("DESTINATION");
                    String sessionStartTime = rs.getString("SESSION_START_TIME");
                    String sessionEndTime = rs.getString("SESSION_END_TIME");

                    // Determine session date
                    String sessionDate = sessionStartTime.substring(0, 10); // Extract "dd-MM-yyyy" from timestamp

                    // Add session to the appropriate section
                    runOnUiThread(() -> addSession(sessionId, sessionDate, startLocation, destination, sessionStartTime, sessionEndTime));
                }

                if (!foundSessions) {
                    runOnUiThread(() -> showToast("No sessions found for this email"));
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

    private void addSession(String sessionId, String sessionDate, String startLocation, String destination, String startTime, String endTime) {
        // Create or find the appropriate section view
        LinearLayout sectionLayout = findOrCreateSection(sessionDate);

        // Create session view as a button
        LinearLayout sessionLayout = new LinearLayout(this);
        sessionLayout.setOrientation(LinearLayout.VERTICAL);
        sessionLayout.setBackground(getResources().getDrawable(R.drawable.rounded_rectangle));
        sessionLayout.setPadding(16, 16, 16, 16);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 0, 0, 16);
        sessionLayout.setLayoutParams(layoutParams);

        // Set click listener to open AnalyticsActivity
        sessionLayout.setOnClickListener(v -> {
            Intent intent = new Intent(HistoryActivity.this, AnalyticsActivity.class);
            intent.putExtra("email", email);
            intent.putExtra("sessionId", sessionId); // Pass the session ID
            startActivity(intent);
        });

        // Starting Point - Destination (Subheading)
        TextView locationText = new TextView(this);
        locationText.setText(startLocation + " - " + destination);
        locationText.setTextColor(getResources().getColor(android.R.color.white));
        locationText.setTextSize(20);
        locationText.setTypeface(null, Typeface.BOLD);
        sessionLayout.addView(locationText);

        // Drive duration
        TextView durationText = new TextView(this);
        durationText.setText(calculateDuration(startTime, endTime));
        durationText.setTextColor(getResources().getColor(android.R.color.white));
        durationText.setTypeface(null, Typeface.ITALIC);
        sessionLayout.addView(durationText);

        // Session Start Time - End Time
        TextView timeText = new TextView(this);
        timeText.setText(startTime.substring(11) + " - " + endTime.substring(11)); // Show only the time
        timeText.setTextColor(getResources().getColor(android.R.color.white));
        timeText.setTypeface(null, Typeface.ITALIC);
        sessionLayout.addView(timeText);

        // Add the session view to the section layout
        sectionLayout.addView(sessionLayout);
    }


    private LinearLayout findOrCreateSection(String sessionDate) {
        for (int i = 0; i < historyContainer.getChildCount(); i++) {
            View view = historyContainer.getChildAt(i);
            if (view instanceof LinearLayout) {
                LinearLayout sectionLayout = (LinearLayout) view;
                TextView sectionHeader = (TextView) sectionLayout.getChildAt(0);
                if (sessionDate.equals(sectionHeader.getText().toString())) {
                    return sectionLayout;
                }
            }
        }

        // Create new section
        LinearLayout sectionLayout = new LinearLayout(this);
        sectionLayout.setOrientation(LinearLayout.VERTICAL);

        TextView sectionHeader = new TextView(this);
        sectionHeader.setText(sessionDate);
        sectionHeader.setTextSize(20);
        sectionHeader.setTypeface(null, Typeface.BOLD);
        sectionLayout.addView(sectionHeader);

        historyContainer.addView(sectionLayout);
        return sectionLayout;
    }

    private String calculateDuration(String startTime, String endTime) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
            Date startDate = dateFormat.parse(startTime);
            Date endDate = dateFormat.parse(endTime);

            long durationMillis = endDate.getTime() - startDate.getTime();
            long seconds = durationMillis / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            seconds = seconds % 60;
            minutes = minutes % 60;

            return String.format(Locale.getDefault(), "%d hours %d minutes", hours, minutes);
        } catch (Exception e) {
            e.printStackTrace();
            return "Unknown duration";
        }
    }



    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(HistoryActivity.this, message, Toast.LENGTH_SHORT).show());
    }
}
