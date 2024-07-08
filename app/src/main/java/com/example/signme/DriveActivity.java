package com.example.signme;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DriveActivity extends AppCompatActivity {

    ConnectionClass connectionClass;
    Connection con;

    private AutoCompleteTextView startingPointAutoComplete;
    private AutoCompleteTextView destinationAutoComplete;
    private Button startButton;
    private ImageView profileImage;
    private TextView profileName;
    private String email;

    @Override
    @SuppressLint("ClickableViewAccessibility")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drive);

        connectionClass = new ConnectionClass();

        // Initialize views
        startingPointAutoComplete = findViewById(R.id.startingPoint);
        destinationAutoComplete = findViewById(R.id.destination);
        startButton = findViewById(R.id.startButton);
        profileImage = findViewById(R.id.profileImage);
        profileName = findViewById(R.id.profileName);

        // Setup autocomplete for Starting Point and Destination using custom adapter
        ArrayAdapter<String> adapter = new CityAutoCompleteAdapter(this, getSriLankanCities());
        startingPointAutoComplete.setAdapter(adapter);
        destinationAutoComplete.setAdapter(adapter);

        // Receive the email from HomeActivity
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("email")) {
            email = intent.getStringExtra("email");
            loadProfileData(email);
        } else {
            Toast.makeText(this, "Email not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Set click listener for Start button
        startButton.setOnClickListener(v -> startSession());

        // Set click listener for arrow icon (drawableEnd) in startingPointAutoComplete
        startingPointAutoComplete.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (startingPointAutoComplete.getRight() - startingPointAutoComplete.getCompoundDrawables()[2].getBounds().width())) {
                    startingPointAutoComplete.showDropDown();
                    return true;
                }
            }
            return false;
        });

        // Set click listener for arrow icon (drawableEnd) in destinationAutoComplete
        destinationAutoComplete.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (destinationAutoComplete.getRight() - destinationAutoComplete.getCompoundDrawables()[2].getBounds().width())) {
                    destinationAutoComplete.showDropDown();
                    return true;
                }
            }
            return false;
        });

        // Set click listener for profile layout to open ProfileActivity
    }

    // Method to load profile data from the database
    private void loadProfileData(String email) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            try {
                con = connectionClass.CONN();
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

    // Method to start a new session
    private void startSession() {
        String startingPoint = startingPointAutoComplete.getText().toString().trim();
        String destination = destinationAutoComplete.getText().toString().trim();
        String vehicleType = "Vehicle";

        if (startingPoint.isEmpty() || destination.isEmpty()) {
            Toast.makeText(this, "Please enter Starting Point and Destination", Toast.LENGTH_SHORT).show();
            return;
        }

        String sessionId = generateSessionId();

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String sessionStartTime = sdf.format(new Date());

        saveSessionData(email, sessionId, startingPoint, destination, vehicleType, sessionStartTime);

        // Start SessionActivity and pass email address
        Intent intent = new Intent(DriveActivity.this, SessionActivity.class);
        intent.putExtra("email", email);
        startActivity(intent);
    }

    private void saveSessionData(String email, String sessionId, String startingPoint, String destination, String vehicleType, String sessionStartTime) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            try {
                con = connectionClass.CONN();
                if (con == null) {
                    runOnUiThread(() -> Toast.makeText(this, "Error in connection with MySQL server", Toast.LENGTH_SHORT).show());
                    return;
                }

                String query = "INSERT INTO session (EMAIL, SESSION_ID, START_LOCATION, DESTINATION, VEHICLE_TYPE, SESSION_START_TIME) VALUES (?, ?, ?, ?, ?, ?)";
                PreparedStatement stmt = con.prepareStatement(query);
                stmt.setString(1, email);
                stmt.setString(2, sessionId);
                stmt.setString(3, startingPoint);
                stmt.setString(4, destination);
                stmt.setString(5, vehicleType);
                stmt.setString(6, sessionStartTime);

                int rowsInserted = stmt.executeUpdate();
                runOnUiThread(() -> {
                    if (rowsInserted > 0) {
                        Toast.makeText(this, "Session started and saved successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to start session", Toast.LENGTH_SHORT).show();
                    }
                });

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

    // Method to retrieve Sri Lankan cities for autocomplete suggestions
    private String[] getSriLankanCities() {
        return new String[]{
                "Mihinthale",
                "Dharmalokagama Junction",
                "Matale Junction",
                "Jaffna Junction",
                "Old Town Market",
                "Market",
                "Bank Town",
                "Anuradhapura New Town"
        };
    }

    // Method to generate SESSION_ID in format DDMMYYYY-HHmm
    private String generateSessionId() {
        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy-HHmmss");
        return sdf.format(new Date());
    }

    // Method to navigate to ProfileActivity


    // Custom ArrayAdapter to customize dropdown appearance
    private class CityAutoCompleteAdapter extends ArrayAdapter<String> {

        public CityAutoCompleteAdapter(DriveActivity context, String[] cities) {
            super(context, android.R.layout.simple_dropdown_item_1line, cities);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView textView = (TextView) super.getView(position, convertView, parent);
            textView.setTextColor(getResources().getColor(android.R.color.black));
            return textView;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            TextView textView = (TextView) super.getDropDownView(position, convertView, parent);
            textView.setTextColor(getResources().getColor(android.R.color.black));
            return textView;
        }
    }
}
